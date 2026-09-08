package com.example.DWI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.DWI.model.Cliente;
import com.example.DWI.repository.ClienteRepository;
import com.example.DWI.repository.FormulaRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CrudTests {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ClienteRepository clientes;

    @Autowired
    private FormulaRepository formulas;

    private HttpClient client;

    @BeforeEach
    void setUp() {
        client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .build();
    }

    private HttpResponse<String> get(String path) throws Exception {
        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private HttpResponse<String> post(String path, String body, String html) throws Exception {
        Matcher m = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertTrue(m.find(), "Debe existir el token CSRF en el formulario");

        String csrf = URLEncoder.encode(m.group(1), StandardCharsets.UTF_8);
        String fullBody = (body.isEmpty() ? "" : body + "&") + "_csrf=" + csrf;

        return client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(fullBody))
                        .build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    @Test
    void loginAndCrud() throws Exception {
        // 1. Acceso anónimo redirige al login
        assertEquals(302, get("/clientes").statusCode());

        // 2. Intento de login fallido
        String login = get("/login").body();
        HttpResponse<String> badLogin = post("/login", "username=admin@gmail.com&password=wrong", login);
        assertTrue(badLogin.headers().firstValue("location").orElse("").endsWith("/login?error"),
                "El login fallido debe redirigir a /login?error");

        // 3. Login exitoso con credenciales de la base de datos
        login = get("/login?error").body();
        HttpResponse<String> goodLogin = post("/login", "username=admin@gmail.com&password=admin123", login);
        String location = goodLogin.headers().firstValue("location").orElse("");
        assertTrue(location.contains("/clientes") || location.contains("/dashboard"),
                "El login exitoso debe redirigir a /dashboard o /clientes, pero fue: " + location);

        // 4. Validación de cliente con DNI inválido
        String form = get("/clientes/nuevo").body();
        String datosCliente = "dni=12345678&nombres=Ana&apellidos=Perez&telefono=987654321&direccion=Lima";
        assertEquals(200, post("/clientes/guardar", datosCliente.replace("12345678", "bad"), form).statusCode());
        assertEquals(0, clientes.count());

        // 5. Guardar cliente válido
        assertEquals(302, post("/clientes/guardar", datosCliente, form).statusCode());
        assertEquals(1, clientes.count());
        Long clienteId = clientes.findAll().getFirst().getId();

        // 6. Ver detalle y editar cliente: en la edición solo se actualizan el teléfono
        //    y la dirección; el DNI, los nombres y los apellidos son inmutables.
        //    El teléfono admite 6 dígitos (fijo) y la dirección puede quedar vacía (opcional).
        assertEquals(200, get("/clientes/" + clienteId).statusCode());
        String editForm = get("/clientes/" + clienteId + "/editar").body();
        String datosEdicion = "dni=99999999&nombres=Maria&apellidos=Gonzales&telefono=012345&direccion=";
        assertEquals(302, post("/clientes/guardar", "id=" + clienteId + "&" + datosEdicion, editForm).statusCode());
        Cliente editado = clientes.findById(clienteId).orElseThrow();
        assertNull(editado.getDireccion());
        assertEquals("012345", editado.getTelefono());
        assertEquals("12345678", editado.getDni());
        assertEquals("Ana", editado.getNombres());
        assertEquals("Perez", editado.getApellidos());

        // 6b. Un teléfono con un número de dígitos distinto de 6 o 9 se rechaza.
        assertEquals(200, post("/clientes/guardar",
                "id=" + clienteId + "&dni=99999999&nombres=Maria&apellidos=Gonzales&telefono=1234567&direccion=", editForm).statusCode());
        assertEquals("012345", clientes.findById(clienteId).orElseThrow().getTelefono());

        // 7. Evitar duplicar DNI
        assertEquals(200, post("/clientes/guardar", datosCliente, get("/clientes/nuevo").body()).statusCode());
        assertEquals(1, clientes.count());

        // 8. Crear fórmula asociada al cliente
        String datosFormula = "nombre=Prueba&presentacion=Crema&composicion=Componente&indicaciones=Uso+externo&cliente=" + clienteId;
        String formFormula = get("/formulas/nueva").body();
        assertEquals(302, post("/formulas/guardar", datosFormula, formFormula).statusCode());
        Long formulaId = formulas.findAll().getFirst().getId();

        // 9. Ver y editar fórmula
        assertEquals(200, get("/formulas").statusCode());
        assertEquals(200, get("/formulas/" + formulaId).statusCode());
        String editFormulaForm = get("/formulas/" + formulaId + "/editar").body();
        assertEquals(302, post("/formulas/guardar", "id=" + formulaId + "&" + datosFormula.replace("Prueba", "Actualizada"), editFormulaForm).statusCode());
        assertEquals("Actualizada", formulas.findById(formulaId).orElseThrow().getNombre());

        // 10. No se debe permitir eliminar un cliente con fórmulas asociadas
        post("/clientes/" + clienteId + "/eliminar", "", get("/clientes").body());
        assertTrue(clientes.existsById(clienteId));

        // 11. Eliminar fórmula y luego cliente
        post("/formulas/" + formulaId + "/eliminar", "", get("/formulas").body());
        assertFalse(formulas.existsById(formulaId));

        post("/clientes/" + clienteId + "/eliminar", "", get("/clientes").body());
        assertFalse(clientes.existsById(clienteId));

        // 12. Cerrar sesión
        post("/logout", "", get("/clientes").body());
        assertEquals(302, get("/formulas").statusCode());
    }
}
