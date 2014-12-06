package mx.amib.sistemas.registro.apoderado.service

import grails.transaction.Transactional
import grails.converters.JSON
import mx.amib.sistemas.registro.apoderamiento.model.AutorizadoCNBV
import mx.amib.sistemas.registro.apoderamiento.model.OficioCNBV

@Transactional
class OficioCNBVService {
	class SearchResult {
		def list
		def count
	}
	
	def save(OficioCNBV oficioCNBVInstance, List<String> autorizadosJson){
		
		oficioCNBVInstance.autorizadosCNBV = new HashSet<OficioCNBV>()
		autorizadosJson.each{
			def parsedJson = JSON.parse(it)
			
			AutorizadoCNBV autorizadoCNBV = new AutorizadoCNBV()
			autorizadoCNBV.numeroMatricula = parsedJson.'numeroMatricula'
			autorizadoCNBV.nombreCompleto = parsedJson.'nombreCompleto'
			
			autorizadoCNBV.oficioCNBV = oficioCNBVInstance
			oficioCNBVInstance.autorizadosCNBV.add(autorizadoCNBV)
		}
		
		oficioCNBVInstance.fechaCreacion = new Date()
		oficioCNBVInstance.fechaModificacion = new Date()
		
		oficioCNBVInstance.save(flush:true, failOnError: true)
		
		return oficioCNBVInstance
	}
	
	def update(OficioCNBV oficioCNBVInstance, List<String> autorizadosJson){
		
		List<AutorizadoCNBV> autsToDelete = new ArrayList<AutorizadoCNBV>();
		List<AutorizadoCNBV> autsToAdd = new ArrayList<AutorizadoCNBV>();
		oficioCNBVInstance.autorizadosCNBV.each{
			it.toBeDeleted = true
		}
		autorizadosJson.each{ _autorizadoJson ->
			def parsedJson = JSON.parse(_autorizadoJson)
			AutorizadoCNBV autorizadoCNBV = oficioCNBVInstance.autorizadosCNBV.find{ it.numeroMatricula == parsedJson.'numeroMatricula' }
			if(autorizadoCNBV == null){
				autorizadoCNBV = new AutorizadoCNBV()
				autorizadoCNBV.numeroMatricula = parsedJson.'numeroMatricula'
				autorizadoCNBV.nombreCompleto = parsedJson.'nombreCompleto'
				autsToAdd.add(autorizadoCNBV)
			}
			autorizadoCNBV.toBeDeleted = false
		}
		oficioCNBVInstance.autorizadosCNBV.each{
			if(it.toBeDeleted == true)
			{
				autsToDelete.add(it)
			}
		}
		autsToDelete.each{
			oficioCNBVInstance.removeFromAutorizadosCNBV(it)
			it.delete(flush:true)
		}
		autsToAdd.each{
			it.oficioCNBV = oficioCNBVInstance
			oficioCNBVInstance.autorizadosCNBV.add(it)
		}
		
		oficioCNBVInstance.fechaCreacion = new Date()
		oficioCNBVInstance.fechaModificacion = new Date()
		
		oficioCNBVInstance.save(flush:true, failOnError: true)
		
		return oficioCNBVInstance
	}
	
	def searchByDatosOficio(Integer max, Integer offset, String sort, String order,
							String filterClaveDga, Integer filterFechaDelDia, Integer filterFechaDelMes, Integer filterFechaDelAnio, 
							Integer filterFechaAlDia, Integer filterFechaAlMes, Integer filterFechaAlAnio){
		
		List<String> hqlFilters = new ArrayList<String>();
		String whereKeyword = "where ";
		Boolean whereKeywordNeeded = false;
		StringBuilder sbHql = new StringBuilder()
		Map<String,Object> namedParameters = new HashMap<String,Object>()
		
		Calendar filterCalFechaDel = null
		Calendar filterCalFechaAl = null
		
		if(max == null || max <= 0){
			max = 10
		}
		if(offset == null || offset <= 0){
			offset = 0
		}
		if(sort == null || sort == ""){
			sort = "id"
		}
		else if(["id","claveDga","fechaFinVigencia"].find{ sort == it } == null){
			sort = "id"
		}
		if(order == null || order == ""){
			order = "asc"
		}
		else if(order != "desc" && order != "asc"){
			order = "asc"
		}
		
		//formar fechas
		if( (filterFechaDelDia != null && filterFechaDelMes != null && filterFechaDelAnio != null) &&
			(filterFechaDelDia > 0 && filterFechaDelMes > 0 && filterFechaDelAnio > 0) ){
			filterCalFechaDel = new GregorianCalendar(filterFechaDelAnio,filterFechaDelMes-1,filterFechaDelDia,00,00,00);
		}
		if( (filterFechaAlDia != null && filterFechaAlMes != null && filterFechaAlAnio != null) &&
			(filterFechaAlDia > 0 && filterFechaAlMes > 0 && filterFechaAlAnio > 0) ){
			filterCalFechaAl = new GregorianCalendar(filterFechaAlAnio,filterFechaAlMes-1,filterFechaAlDia,00,00,00);
		}
		
		if(filterClaveDga != null && filterClaveDga != "" ){
			hqlFilters.add("o.claveDga = :claveDga ")
			whereKeywordNeeded = true
			namedParameters.put("claveDga",filterClaveDga)
		}
		
		//rangos de fecha
		//si ambos son nulos, se omite; si uno es nulo, el que no es nulo se toma como unico
		if( filterCalFechaDel != null && filterCalFechaAl == null){
			hqlFilters.add("o.fechaFinVigencia >= :fechaFinVigencia ")
			whereKeywordNeeded = true
			namedParameters.put("fechaFinVigencia",filterCalFechaDel.getTime())
		}
		if( filterCalFechaDel == null && filterCalFechaAl != null){
			hqlFilters.add("o.fechaFinVigencia <= :fechaFinVigencia ")
			whereKeywordNeeded = true
			namedParameters.put("fechaFinVigencia",filterCalFechaAl.getTime())
		}
		if( filterCalFechaDel != null && filterCalFechaAl != null){
			hqlFilters.add("o.fechaFinVigencia between :fechaDel and :fechaAl ")
			whereKeywordNeeded = true
			namedParameters.put("fechaDel",filterCalFechaDel.getTime())
			namedParameters.put("fechaAl",filterCalFechaAl.getTime())
		}
		
		sbHql.append("from OficioCNBV as o ")
		if(whereKeywordNeeded){
			sbHql.append(whereKeyword)
			hqlFilters.each{
				if(it != hqlFilters.last())
					sbHql.append(it).append("and ")
				else
					sbHql.append(it)
			}
		}
		
		def results = new SearchResult()
		
		def totalResults = OficioCNBV.executeQuery("select count(o) " + sbHql.toString(),namedParameters)
		results.count = totalResults[0];
		sbHql.append("order by o.").append(sort).append(" ").append(order)
		results.list =  OficioCNBV.executeQuery(sbHql.toString(),namedParameters,[max: max, offset: offset])
		
		return results
	}
	
	def searchByMatricula(Integer max, Integer offset, String sort, String order, Integer filterMatricula){
		List<String> hqlFilters = new ArrayList<String>();
		String whereKeyword = "where ";
		Boolean whereKeywordNeeded = false;
		StringBuilder sbHql = new StringBuilder()
		Map<String,Object> namedParameters = new HashMap<String,Object>()
		
		if(max == null || max <= 0){
			max = 10
		}
		if(offset == null || offset <= 0){
			offset = 0
		}
		if(sort == null || sort == ""){
			sort = "id"
		}
		else if(["id","claveDga","fechaFinVigencia"].find{ sort == it } == null){
			sort = "id"
		}
		if(order == null || order == ""){
			order = "asc"
		}
		else if(order != "desc" && order != "asc"){
			order = "asc"
		}
		
		if(filterMatricula != null && filterMatricula > 0 ){
			hqlFilters.add("a.numeroMatricula = :numeroMatricula ")
			whereKeywordNeeded = true
			namedParameters.put("numeroMatricula",filterMatricula)
		}
		
		sbHql.append("select a.oficioCNBV from AutorizadoCNBV as a ")
		if(whereKeywordNeeded){
			sbHql.append(whereKeyword)
			hqlFilters.each{
				if(it != hqlFilters.last())
					sbHql.append(it).append("and ")
				else
					sbHql.append(it)
			}
		}
		sbHql.append("order by a.oficioCNBV.").append(sort).append(" ").append(order)
		
		println sbHql.toString()
		println (namedParameters as JSON)
		
		def results = OficioCNBV.executeQuery(sbHql.toString(),namedParameters,[max: max, offset: offset])
		return results
	}
	
	def searchByNombre(Integer max, Integer offset, String sort, String order, String filterNombre){
		
		List<String> hqlFilters = new ArrayList<String>();
		String whereKeyword = "where ";
		Boolean whereKeywordNeeded = false;
		StringBuilder sbHql = new StringBuilder()
		Map<String,Object> namedParameters = new HashMap<String,Object>()
		
		if(max == null || max <= 0){
			max = 10
		}
		if(offset == null || offset <= 0){
			offset = 0
		}
		if(sort == null || sort == ""){
			sort = "id"
		}
		else if(["id","claveDga","fechaFinVigencia"].find{ sort == it } == null){
			sort = "id"
		}
		if(order == null || order == ""){
			order = "asc"
		}
		else if(order != "desc" && order != "asc"){
			order = "asc"
		}
		
		if(filterNombre != null && filterNombre != ""){
			hqlFilters.add("a.nombreCompleto like :nombreCompleto ")
			whereKeywordNeeded = true
			namedParameters.put("nombreCompleto","%"+filterNombre+"%")
		}
		
		sbHql.append("select a.oficioCNBV from AutorizadoCNBV as a ")
		if(whereKeywordNeeded){
			sbHql.append(whereKeyword)
			hqlFilters.each{
				if(it != hqlFilters.last())
					sbHql.append(it).append("and ")
				else
					sbHql.append(it)
			}
		}
		sbHql.append("order by a.oficioCNBV.").append(sort).append(" ").append(order)
		
		println sbHql.toString()
		println (namedParameters as JSON)
		
		def results = OficioCNBV.executeQuery(sbHql.toString(),namedParameters,[max: max, offset: offset])
		return results
	}
}
