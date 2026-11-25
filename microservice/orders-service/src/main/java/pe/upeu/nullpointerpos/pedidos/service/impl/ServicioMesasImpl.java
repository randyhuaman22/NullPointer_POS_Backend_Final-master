package pe.upeu.nullpointerpos.pedidos.service.impl;

import pe.upeu.nullpointerpos.pedidos.dto.response.RespuestaMesa;
import pe.upeu.nullpointerpos.pedidos.mapper.MesaMapper;
import pe.upeu.nullpointerpos.pedidos.repository.MesaRepositorio;
import pe.upeu.nullpointerpos.pedidos.service.ServicioMesas;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServicioMesasImpl implements ServicioMesas {
    private final MesaRepositorio repositorio;
    private final MesaMapper mapper;
    public ServicioMesasImpl(MesaRepositorio repositorio, MesaMapper mapper) { this.repositorio = repositorio; this.mapper = mapper; }
    @Override public List<RespuestaMesa> listarMesas() {
        return repositorio.listarActivas().stream().map(mapper::aRespuesta).toList();
    }
}
