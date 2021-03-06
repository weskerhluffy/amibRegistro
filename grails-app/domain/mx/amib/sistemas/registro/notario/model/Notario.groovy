package mx.amib.sistemas.registro.notario.model

import mx.amib.sistemas.registro.apoderamiento.model.Poder;
import mx.amib.sistemas.registro.apoderamiento.model.Revocacion;

class Notario {

	Integer idEntidadFederativa
	Integer numeroNotario
	Integer seqNotario
	
	String nombre
	String apellido1
	String apellido2
	
	Boolean vigente
	
	Date fechaCreacion
	Date fechaModificacion

	String nombreEntidadFederativa
	
	static transients = ['nombreEntidadFederativa']
	static hasMany = [poderes: Poder,
	                  revocaciones: Revocacion]

	static mapping = {
		table 't201_t_notario'
		
		id generator: "identity"
		
		idEntidadFederativa column:'id_f_entidadfed'
		numeroNotario column:'nu_numnotariope'
		seqNotario column:'nu_seq'
		
		nombre column:'nb_nombre'
		apellido1 column:'nb_apellido1'
		apellido2 column:'nb_apellido2'
		
		vigente column:'st_vigente'
		
		fechaCreacion column:'fh_creacion'
		fechaModificacion column:'fh_modificacion'
		
		
	}

	static constraints = {
		idEntidadFederativa unique: ["numeroNotario","seqNotario"]
		nombre maxSize: 100
		apellido1 maxSize: 80
		apellido2 maxSize: 80
		fechaCreacion nullable: true
		fechaModificacion nullable: true
	}
}
