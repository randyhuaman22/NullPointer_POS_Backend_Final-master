package pe.upeu.nullpointerpos.productos.dto.response;

import java.time.OffsetDateTime;
import java.util.Map;

public record RespuestaAuditoriaCatalogo(Long idAuditoria, String tipoEntidad, Long idEntidad,
                                         String accion, String usuarioActor,
                                         Map<String, Object> datos, OffsetDateTime fechaCreacion) { }
