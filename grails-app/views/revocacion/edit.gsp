<%@ page contentType="text/html;charset=UTF-8" %>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'revocacion.label', default: 'Revocación')}" />
		<title>Registro 0.1 - Edición de revocación</title>
	</head>
	<body>
		<a id="anchorForm"></a>
		<ul class="breadcrumb">
			<li><a href="#">Servicios</a><span class="divider"></span></li>
			<li><a href="#">Editar revocación</a></li>
		</ul>
	
		<h2><strong>Editar revocación</strong></h2>

		<g:form id="frmApp" url="[resource:revocacionInstance, action:'update']" method="PUT" class="form-horizontal" role="form">
			<g:render template="form"/>
		</g:form>
		<g:render template="formJs"/>
		
	</body>
</html>
