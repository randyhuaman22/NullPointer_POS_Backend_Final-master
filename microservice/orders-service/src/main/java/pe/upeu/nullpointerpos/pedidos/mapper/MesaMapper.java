package pe.upeu.nullpointerpos.pedidos.mapper;

import pe.upeu.nullpointerpos.pedidos.entity.Mesa;
import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaMesa;
import org.springframework.stereotype.Component;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MesaMapper {
    public Mesa desdeFila(ResultSet rs) throws SQLException {
        return new Mesa(rs.getLong("id_mesa"), rs.getString("codigo_mesa"), rs.getString("nombre_area"),
                rs.getInt("capacidad"), rs.getString("estado"), rs.getBoolean("activo"));
    }
    public RespuestaMesa aRespuesta(Mesa mesa) {
        return new RespuestaMesa(mesa.idMesa(), mesa.codigoMesa(), mesa.nombreArea(), mesa.capacidad(), mesa.estado(), mesa.activo());
    }
}
