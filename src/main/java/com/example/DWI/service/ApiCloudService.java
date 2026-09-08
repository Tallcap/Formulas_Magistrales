package com.example.DWI.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servicio de integración con la nube de servicios externos (ApiCloud / RENIEC).
 * Consulta en tiempo real los datos oficiales de personas naturales a partir del DNI (8 dígitos).
 */
@Service
public class ApiCloudService {

    private static final Logger log = LoggerFactory.getLogger(ApiCloudService.class);

    private final RestClient restClient;
    private final String baseUrl;
    private final String apiToken;

    public ApiCloudService(
            @Value("${apicloud.reniec.base-url:${apis.reniec.base-url:https://miapi.cloud/v1}}") String baseUrl,
            @Value("${apicloud.reniec.token:${apis.reniec.token:}}") String apiToken
    ) {
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl.trim() : "https://miapi.cloud/v1";
        this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
        this.apiToken = apiToken != null ? apiToken.trim() : "";
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> consultar(String dni) {
        if (dni == null || !dni.matches("\\d{8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El DNI debe tener exactamente 8 dígitos numéricos");
        }

        if (apiToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Configure el token de ApiCloud antes de consultar");
        }

        try {
            log.info("ApiCloudService: Consultando DNI {} en ApiCloud ({})", dni, baseUrl);

            Map<String, Object> respuesta;
            if (baseUrl.contains("apis.net.pe")) {
                respuesta = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/dni")
                                .queryParam("numero", dni)
                                .build())
                        .header("Authorization", "Bearer " + apiToken)
                        .retrieve()
                        .body(Map.class);
            } else {
                respuesta = restClient.get()
                        .uri("/dni/{dni}", dni)
                        .header("Authorization", "Bearer " + apiToken)
                        .retrieve()
                        .body(Map.class);
            }

            if (respuesta == null || respuesta.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron datos para el DNI indicado");
            }

            return respuesta;
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("ApiCloudService: Token de autorización inválido o vencido", e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El token de ApiCloud es inválido o ha vencido");
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("ApiCloudService: DNI {} no existe en los registros oficiales", dni);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontraron registros para el DNI ingresado");
        } catch (RestClientResponseException e) {
            log.error("ApiCloudService: Error devuelto por el servidor remoto: {}", e.getStatusCode(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "El servicio de ApiCloud respondió con código HTTP " + e.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("ApiCloudService: Error de conectividad", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo establecer comunicación con ApiCloud");
        }
    }
}
