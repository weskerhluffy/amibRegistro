package mx.amib.sistemas.registro.apoderado.service

import java.util.Date;
import java.util.HashMap;

import grails.transaction.Transactional
import grails.converters.JSON
import grails.plugins.rest.client.RestBuilder
import mx.amib.sistemas.registro.apoderamiento.model.Apoderado
import mx.amib.sistemas.registro.apoderamiento.model.AutorizadoCNBV
import mx.amib.sistemas.registro.apoderamiento.model.DocumentoRespaldoPoder
import mx.amib.sistemas.registro.apoderamiento.model.Poder
import mx.amib.sistemas.registro.apoderamiento.model.catalog.TipoDocumentoRespaldoPoder
import mx.amib.sistemas.external.catalogos.service.EntidadFinancieraService;
import mx.amib.sistemas.external.catalogos.service.SepomexService
import mx.amib.sistemas.external.documentos.service.DocumentoPoderRepositorioTO;
import mx.amib.sistemas.external.documentos.service.DocumentoRepositorioService;
import mx.amib.sistemas.external.documentos.service.DocumentoRepositorioTO;
import mx.amib.sistemas.registro.notario.model.Notario
import mx.amib.sistemas.registro.notario.service.NotarioService
import groovy.json.StringEscapeUtils

@Transactional
class PoderService {

	NotarioService notarioService
	DocumentoRepositorioService documentoRepositorioService
	SepomexService sepomexService
	EntidadFinancieraService entidadFinancieraService
	
	
    def nuevoPoderPorGrupoFinanciero(long idGrupoFinanciero) {
		
    }
	
	def nuevoPoderPorInstitucion(long idInstitucion) {
		
	}
	
	Collection<TipoDocumentoRespaldoPoder> obtenerListadoTipoDocumentoRespaldoPoder() {
		return TipoDocumentoRespaldoPoder.findAllByVigente(true)
	}
	
	//Contruye el modelo a guardar
	Poder buildFromParamsToSave(Poder poder, int notarioNumero, int notarioIdEntidadFederativa, 
								List<Integer> apoderadosIdAutorizadoCNBV, List<DocumentoRespaldoPoderTO> documentosGuardados){
								
		Notario n = notarioService.obtenerNotario(notarioIdEntidadFederativa, notarioNumero)
		poder.notario = n
		
		poder.apoderados = new HashSet<Apoderado>()
		apoderadosIdAutorizadoCNBV.each {
			AutorizadoCNBV acnbv = AutorizadoCNBV.get(it)
						
			Apoderado a = new Apoderado()
			a.poder = poder
			a.autorizado = acnbv
			
			poder.apoderados.add(a)
		}
		
		poder.documentosRespaldoPoder = new HashSet<DocumentoRespaldoPoder>()
		documentosGuardados.each {
			DocumentoRespaldoPoder drp = new DocumentoRespaldoPoder()
			
			drp.uuidDocumentoRepositorio = it.uuid
			drp.tipoDocumentoRespaldoPoder = TipoDocumentoRespaldoPoder.get(it.idTipoDocumento)
			drp.poder = poder
			
			poder.documentosRespaldoPoder.add(drp)
		}
		
		//en cuanto se implemente la sesión con atributos
		//debera adecuarse de acuerdo a si quien esta subiendo el cambio
		//es una entidadFinanciera o institucion
		//o en caso de ser un ADMON, se omite este paso
		poder.esRegistradoPorGrupoFinanciero = true
		poder.idGrupofinanciero = 6
		poder.idInstitucion = -1
		
		//fechas
		poder.fechaCreacion = new Date()
		poder.fechaModificacion = new Date()
		
		return poder
	}
	
	//Obtiene errores de acuerdo a validaciones de reglas de negocio
	Collection<PoderIntegrityError> validateBusinessIntegrity(Poder poder){
		return null
	}
	
	//Guarda el modelo
	Poder save(Poder poder){
		//guarda los documentos en el repositorio
		this.saveDocsOnRepository(poder)
		//guarda en la BD
		return poder.save(flush:true)
	}
	
	//Envía los documentos pertenecientes al Poder (que 
	//hayas sido cargados a temporales) al repositorio "amibDocumentos"
	//NOTA: Los datos cargados son unicamente auxiliares para la 
	//búsqueda del documento en el respositorio
	//Si hay un cambio fuera de la aplicación (a nivel BD), se tendrá
	//que hacer manualmente en la base del repositorio
	def saveDocsOnRepository(Poder poder){
		
		List<DocumentoRepositorioTO> docsEnviar = new ArrayList<DocumentoRepositorioTO>()
		
		poder.documentosRespaldoPoder.each{
			DocumentoPoderRepositorioTO docEnviar = new DocumentoPoderRepositorioTO()
			
			docEnviar.id = null
			docEnviar.uuid = it.uuidDocumentoRepositorio
			
			docEnviar.tipoDocumentoRespaldo = it.tipoDocumentoRespaldoPoder.descripcion
			docEnviar.representanteLegalNombre = it.poder.representanteLegalNombre
			docEnviar.representanteLegalApellido1 = it.poder.representanteLegalApellido1
			docEnviar.representanteLegalApellido2 = it.poder.representanteLegalApellido2
			docEnviar.esRegistradoPorGrupoFinanciero = it.poder.esRegistradoPorGrupoFinanciero
			docEnviar.numeroEscritura = it.poder.numeroEscritura
			docEnviar.fechaApoderamiento = it.poder.fechaApoderamiento
			docEnviar.jsonApoderados = it.poder.apoderados as JSON
			docEnviar.jsonNotario = it.poder.notario as JSON
			
			docEnviar.jsonApoderados = StringEscapeUtils.unescapeJava(docEnviar.jsonApoderados)
			docEnviar.jsonNotario = StringEscapeUtils.unescapeJava(docEnviar.jsonNotario)
				
			if(it.poder.esRegistradoPorGrupoFinanciero == true)
			{
				docEnviar.jsonGrupoFinanciero = entidadFinancieraService.obtenerGrupoFinanciero(it.poder.idGrupofinanciero) as JSON 
				docEnviar.jsonInstitucion = null
				docEnviar.jsonGrupoFinanciero = StringEscapeUtils.unescapeJava(docEnviar.jsonGrupoFinanciero)
			}
			else
			{				
				docEnviar.jsonGrupoFinanciero = null
				docEnviar.jsonInstitucion = entidadFinancieraService.obtenerInstitucion(it.poder.idInstitucion) as JSON 
				docEnviar.jsonInstitucion = StringEscapeUtils.unescapeJava(docEnviar.jsonInstitucion)
			}

			docsEnviar.add(docEnviar)
		}
		documentoRepositorioService.enviarDocumentosArchivoTemporal(docsEnviar)
	}
}

class DocumentoRespaldoPoderTO {
	String sessionId
	
	Long id
	String uuid
	Long idTipoDocumento
	String tipoDocumento
}

class PoderIntegrityError {
	String clave
	String descripcion
}