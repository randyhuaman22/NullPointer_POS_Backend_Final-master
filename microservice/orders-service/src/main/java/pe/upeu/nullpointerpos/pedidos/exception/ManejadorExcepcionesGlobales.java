package pe.upeu.nullpointerpos.pedidos.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ManejadorExcepcionesGlobales {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ManejadorExcepcionesGlobales.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorApi> manejarEstado(ResponseStatusException ex, HttpServletRequest solicitud) {
        HttpStatus estado = HttpStatus.valueOf(ex.getStatusCode().value());
        return construir(estado, ex.getReason() == null ? estado.getReasonPhrase() : ex.getReason(), solicitud);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApi> manejarValidacion(MethodArgumentNotValidException ex, HttpServletRequest solicitud) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatearErrorCampo).collect(Collectors.joining("; "));
        return construir(HttpStatus.BAD_REQUEST,
                mensaje.isBlank() ? "Los datos enviados no son válidos." : mensaje, solicitud);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> manejarConflicto(DataIntegrityViolationException ex, HttpServletRequest solicitud) {
        return construir(HttpStatus.CONFLICT,
                "Conflicto de datos: revisa duplicados o reglas de integridad, por ejemplo una mesa ocupada.", solicitud);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorApi> manejarSolicitudIlegible(HttpMessageNotReadableException ex, HttpServletRequest solicitud) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo JSON está vacío o no tiene el formato esperado.", solicitud);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorApi> manejarErrorInesperado(Exception ex, HttpServletRequest solicitud) {
        LOG.error("Error no controlado en {}", solicitud.getRequestURI(), ex);
        return construir(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Revisa los registros del servicio.", solicitud);
    }

    private String formatearErrorCampo(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorApi> construir(HttpStatus estado, String mensaje, HttpServletRequest solicitud) {
        return ResponseEntity.status(estado).body(new ErrorApi(
                Instant.now(), estado.value(), estado.getReasonPhrase(), mensaje, solicitud.getRequestURI()));
    }
}
