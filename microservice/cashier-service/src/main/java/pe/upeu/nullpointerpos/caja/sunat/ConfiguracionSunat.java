package pe.upeu.nullpointerpos.caja.sunat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Parámetros de conexión para la facturación electrónica con SUNAT. */
@Configuration
@ConfigurationProperties(prefix = "app.sunat")
public class ConfiguracionSunat {
    private String ambiente = "BETA";
    private String rucEmisor = "20000000001";
    private String razonSocial = "POLLERIA Y PARRILLAS WENDY'S FOOD S.A.C.";
    private String nombreComercial = "WENDY'S FOOD";
    private String direccion = "AV. REVOLUCION NRO. 1234 - LIMA, PERU";
    private String ubigeo = "150101";
    private String usuarioSol = "MODDATOS";
    private String claveSol = "MODDATOS";
    private String endpointBeta = "https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService";

    private String certificadoRuta = "";
    private String claveCertificado = "";
    private String certificadoAlias = "";
    public String getCertificadoRuta() { return certificadoRuta; }
    public void setCertificadoRuta(String value) { certificadoRuta = value; }
    public String getClaveCertificado() { return claveCertificado; }
    public void setClaveCertificado(String value) { claveCertificado = value; }
    public String getCertificadoAlias() { return certificadoAlias; }
    public void setCertificadoAlias(String value) { certificadoAlias = value; }

    public String getAmbiente() { return ambiente; }
    public void setAmbiente(String ambiente) { this.ambiente = ambiente; }
    public String getRucEmisor() { return rucEmisor; }
    public void setRucEmisor(String rucEmisor) { this.rucEmisor = rucEmisor; }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getUbigeo() { return ubigeo; }
    public void setUbigeo(String ubigeo) { this.ubigeo = ubigeo; }
    public String getUsuarioSol() { return usuarioSol; }
    public void setUsuarioSol(String usuarioSol) { this.usuarioSol = usuarioSol; }
    public String getClaveSol() { return claveSol; }
    public void setClaveSol(String claveSol) { this.claveSol = claveSol; }
    public String getEndpointBeta() { return endpointBeta; }
    public void setEndpointBeta(String endpointBeta) { this.endpointBeta = endpointBeta; }
}
