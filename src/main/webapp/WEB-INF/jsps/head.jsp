<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="url">${pageContext.request.requestURL}</c:set>
<base href="${fn:substring(url, 0, fn:length(url) - fn:length(pageContext.request.requestURI))}${pageContext.request.contextPath}/" />

<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<meta name="description" content="Decoder tool for EMV credit card data. Started with TVR (Terminal Verification Results) and grew to an APDU trace."/>
<meta name="og:title" content="TVR Decoder"/>
<meta name="og:description" content="Decoder tool for EMV credit card data. Started with TVR (Terminal Verification Results) and grew to an APDU trace."/>
<meta name="og:image" content="http://tvr-decoder.appspot.com/chip.png"/>
<script src="jquery-1.11.1.min.js"></script>
<script src="bootstrap.min.js"></script>
<script src="app.js"></script>
<link rel="stylesheet" href="css/bootstrap.min.css"/>
<link rel="stylesheet" href="css/bootstrap-theme.min.css"/>
<link rel="stylesheet" href="css/font-awesome-face.css"/>
<link rel="stylesheet" href="tvr.css" />
