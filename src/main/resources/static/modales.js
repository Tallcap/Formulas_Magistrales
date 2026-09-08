const modal = document.querySelector("#editor-modal");
if (modal) {
  const editor = bootstrap.Modal.getOrCreateInstance(modal);
  let trigger;
  const form = modal.querySelector("form");
  const title = modal.querySelector("#modal-title");

  const inputNombres = modal.querySelector("#nombres");
  const inputApellidos = modal.querySelector("#apellidos");
  const inputDni = modal.querySelector("#dni");
  const inputDireccion = modal.querySelector("#direccion");
  const mensajeDni = modal.querySelector("#dni-mensaje");

  function unlockNombresApellidos() {
    if (inputNombres) {
      inputNombres.readOnly = false;
      inputNombres.classList.remove("bg-secondary-subtle");
      inputNombres.removeAttribute("title");
    }
    if (inputApellidos) {
      inputApellidos.readOnly = false;
      inputApellidos.classList.remove("bg-secondary-subtle");
      inputApellidos.removeAttribute("title");
    }
  }

  function lockNombresApellidos() {
    if (inputNombres) {
      inputNombres.readOnly = true;
      inputNombres.classList.add("bg-secondary-subtle");
      inputNombres.setAttribute("title", "Campo bloqueado: proviene de la consulta oficial de RENIEC");
    }
    if (inputApellidos) {
      inputApellidos.readOnly = true;
      inputApellidos.classList.add("bg-secondary-subtle");
      inputApellidos.setAttribute("title", "Campo bloqueado: proviene de la consulta oficial de RENIEC");
    }
  }

  // En modo edición, el DNI, los nombres y los apellidos son datos de identidad
  // y no pueden modificarse: solo se permite actualizar teléfono y dirección.
  function lockIdentityFields() {
    const buscar = modal.querySelector("#buscar-dni");
    if (inputDni) {
      inputDni.readOnly = true;
      inputDni.classList.add("bg-secondary-subtle");
      inputDni.setAttribute("title", "El DNI no puede modificarse al editar un cliente");
    }
    if (buscar) buscar.disabled = true;
    if (inputNombres) {
      inputNombres.readOnly = true;
      inputNombres.classList.add("bg-secondary-subtle");
      inputNombres.setAttribute("title", "Los nombres no pueden modificarse al editar un cliente");
    }
    if (inputApellidos) {
      inputApellidos.readOnly = true;
      inputApellidos.classList.add("bg-secondary-subtle");
      inputApellidos.setAttribute("title", "Los apellidos no pueden modificarse al editar un cliente");
    }
  }

  function unlockIdentityFields() {
    const buscar = modal.querySelector("#buscar-dni");
    if (inputDni) {
      inputDni.readOnly = false;
      inputDni.classList.remove("bg-secondary-subtle");
      inputDni.removeAttribute("title");
    }
    if (buscar) buscar.disabled = false;
    unlockNombresApellidos();
  }

  function openEditor(button, editing) {
    form.reset();
    for (const field of form.elements) {
      if (!field.name || field.name === "_csrf" || field.type === "submit") continue;
      field.value = editing ? (button.getAttribute("data-" + field.name) ?? "") : "";
      field.removeAttribute("aria-invalid");
      field.classList.remove("fieldError", "is-invalid");
    }
    modal.querySelectorAll(".error-text").forEach(error => { error.textContent = ""; });
    title.textContent = editing ? modal.dataset.editTitle : modal.dataset.createTitle;
    trigger = button;

    // Al abrir el modal se restablece el estado editable; en edición se
    // bloquean DNI, nombres y apellidos (solo teléfono y dirección son editables).
    unlockIdentityFields();
    if (editing) lockIdentityFields();
    if (mensajeDni) mensajeDni.textContent = "";

    editor.show();
  }

  document.querySelectorAll("[data-new-modal]").forEach(button =>
    button.addEventListener("click", () => openEditor(button, false)));
  document.querySelectorAll("[data-edit-modal]").forEach(button =>
    button.addEventListener("click", () => openEditor(button, true)));

  modal.addEventListener("shown.bs.modal", () => {
    modal.querySelector('input:not([type="hidden"]), select, textarea')?.focus();
  });

  modal.addEventListener("hidden.bs.modal", () => {
    trigger?.focus();
  });

  form.addEventListener("submit", () => {
    form.querySelectorAll('button:not([type="button"])').forEach(button => { button.disabled = true; });
  });

  // Si el usuario modifica el DNI después de haber buscado, se desbloquean los nombres
  if (inputDni) {
    inputDni.addEventListener("input", () => {
      unlockNombresApellidos();
      if (mensajeDni && mensajeDni.textContent) {
        mensajeDni.textContent = "";
      }
    });
  }

  if (modal.dataset.autoOpen === "true") editor.show();

  // Búsqueda de DNI en RENIEC vía ApiCloud
  const buscarDni = document.querySelector("#buscar-dni");
  if (buscarDni) {
    buscarDni.addEventListener("click", async () => {
      const dni = inputDni ? inputDni.value.trim() : "";
      if (!/^\d{8}$/.test(dni)) {
        if (mensajeDni) {
          mensajeDni.textContent = "Ingresa un DNI válido de 8 dígitos numéricos.";
          mensajeDni.className = "form-text text-danger";
        }
        return;
      }

      buscarDni.disabled = true;
      buscarDni.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Consultando...';
      if (mensajeDni) mensajeDni.textContent = "";

      try {
        const response = await fetch(`/clientes/dni/${dni}`);
        const json = await response.json();
        if (!response.ok) {
          throw new Error(json.detail || json.mensaje || json.message || "No se encontraron datos para el DNI.");
        }
        if (json.success === false) {
          throw new Error(json.mensaje || json.message || "No se encontraron datos para el DNI ingresado.");
        }

        const data = json.datos || json.data || json;
        const nombres = data.nombres || data.nombre || "";
        const paterno = data.ape_paterno || data.apellidoPaterno || data.apellido_paterno || "";
        const materno = data.ape_materno || data.apellidoMaterno || data.apellido_materno || "";
        const completo = data.nombreCompleto || data.nombre_completo || "";

        // Autocompletar y bloquear campos de Nombres y Apellidos
        if (inputNombres) {
          inputNombres.value = nombres || completo;
        }
        if (inputApellidos) {
          inputApellidos.value = [paterno, materno].filter(Boolean).join(" ") || (completo && !nombres ? completo : "");
        }
        lockNombresApellidos();

        // Autocompletar Dirección: ÚNICAMENTE la dirección exacta (sin distrito ni departamento)
        let direccionApi = "";
        if (data.domiciliado && typeof data.domiciliado.direccion === "string") {
          direccionApi = data.domiciliado.direccion.trim();
        } else if (typeof data.direccion === "string") {
          direccionApi = data.direccion.trim();
        }

        if (direccionApi && inputDireccion) {
          inputDireccion.value = direccionApi;
        }

        // Mensaje informativo
        if (mensajeDni) {
          if (direccionApi) {
            mensajeDni.innerHTML = '<i class="bi bi-shield-check text-success me-1"></i> Datos y dirección oficiales cargados. Nombres y apellidos bloqueados contra edición.';
          } else {
            mensajeDni.innerHTML = '<i class="bi bi-shield-check text-success me-1"></i> Datos cargados. Nombres y apellidos bloqueados contra edición.';
          }
          mensajeDni.className = "form-text text-success";
        }
      } catch (error) {
        unlockNombresApellidos();
        if (mensajeDni) {
          mensajeDni.textContent = error.message;
          mensajeDni.className = "form-text text-danger";
        }
      } finally {
        buscarDni.disabled = false;
        buscarDni.innerHTML = '<i class="bi bi-search"></i> Buscar DNI';
      }
    });
  }
}
