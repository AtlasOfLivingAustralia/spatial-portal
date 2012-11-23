<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<jsp:root xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core" version="2.0">
	<registryObjects
		xmlns="http://ands.org.au/standards/rif-cs/registryObjects"
		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		xsi:schemaLocation="http://ands.org.au/standards/rif-cs/registryObjects http://services.ands.org.au/documentation/rifcs/schema/registryObjects.xsd">

	<c:forEach var="layer" items="${layers}">
		<registryObject group="Atlas of Living Australia">
		 	<key>ala.org.au/uid_${layer.uid}</key>
			<originatingSource>${layer.metadatapath}</originatingSource>
				<collection type="dataset">
				<identifier type="local">ala.org.au/uid_${layer.uid}</identifier>
				<name type="abbreviated">
					<namePart>${layer.name}</namePart>
				</name>
				<name type="alternative">
					<namePart>${layer.description}</namePart>
				</name>
				<name type="primary">
					<namePart>${layer.description}</namePart>
				</name>
				<location>
					<address>
						<electronic type="url">
							<value>http://spatial.ala.org.au/layers</value>
						</electronic>
					</address>
				</location>
				<relatedObject>
					<key>Contributor:Atlas of Living Australia</key>
					<relation type="hasCollector" />
				</relatedObject>
				<subject type="anzsrc-for">0502</subject>
				<subject type="local">${layer.classification1}</subject>
				<c:if test="${layer.classification2 ne null}">
				<subject type="local">${layer.classification1}</subject>
				</c:if>
				<description type="full">${layer.notes}</description>
				<relatedInfo type="website">
					<identifier type="uri">${layer.metadatapath}</identifier>
					<title>Further metadata</title>
				</relatedInfo>
				<relatedInfo type="website">
					<identifier type="uri">${layer.source_link}</identifier>
					<title>Original source of this data</title>
				</relatedInfo>
				<rights>
					<licence
						rightsUri="${layer.licence_link}">${layer.licence_notes}</licence>
				</rights>
				<coverage>
					<spatial type="iso19139dcmiBox">northlimit=${layer.maxlatitude}; southlimit=${layer.minlatitude}; westlimit=${layer.minlongitude}; eastLimit=${layer.maxlongitude}; projection=WGS84</spatial>
				</coverage>
			</collection>
		</registryObject>
	</c:forEach>

	</registryObjects>
</jsp:root>