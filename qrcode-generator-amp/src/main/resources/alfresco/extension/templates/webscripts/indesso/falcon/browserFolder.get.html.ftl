<!DOCTYPE html>
<html lang="en">

<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
	
	<title>Document List</title>

	<!-- Google font -->
	<link href="https://fonts.googleapis.com/css?family=Montserrat:400,700,900" rel="stylesheet">
	
	<script src="${hostname}/share/res/components/falcon/assets/jquery3.5.1/jquery.min.js"></script>
	<script src="${hostname}/share/res/components/falcon/assets/bootstrap3.3.7/bootstrap.min.js"></script>

	<script src="${hostname}/share/res/components/falcon/assets/kendoui-2019/js/kendo.all.min.js"></script>
    <script src="${hostname}/share/res/components/falcon/assets/jszip.min.js"></script>

    <script src="${hostname}/share/res/components/falcon/assets/knockout/knockout-3.4.0.js"></script>
    <script src="${hostname}/share/res/components/falcon/assets/knockout/knockout.mapping.js"></script>
    <script src="${hostname}/share/res/components/falcon/assets/knockout-kendo-0.97.min.js"></script>
    <script src="${hostname}/share/res/components/falcon/qrcode/browsefolder/browsefolder.js"></script>

	<link rel="stylesheet" href="${hostname}/share/res/components/falcon/assets/fontawesome-free-5.13.1/css/all.min.css" type="text/css" />
	<link rel="stylesheet" href="${hostname}/share/res/components/falcon/assets/bootstrap3.3.7/bootstrap.min.css" TYPE="text/css">
    <link rel="stylesheet" href="${hostname}/share/res/components/falcon/assets/kendoui-2019/styles/kendo.common.min.css" />
    <link rel="stylesheet" href="${hostname}/share/res/components/falcon/assets/kendoui-2019/styles/kendo.metro.min.css" />
    <link rel="stylesheet" href="${hostname}/share/res/components/falcon/assets/kendoui-2019/styles/kendo.metro.mobile.min.css" />
    <link rel="stylesheet" href="${hostname}/share/res/components/falcon/assets/kendoui-2019/styles/kendo.common-bootstrap.min.css" />
    <link rel="stylesheet" href="${hostname}/share/res/components/falcon/assets/kendoui-2019/styles/kendo.mobile.all.min.css" />
    <link rel="stylesheet" href="${hostname}/share/res/components/falcon/assets/kendoui-2019/styles/kendo.mobile.common.min.css" />

    <link rel="stylesheet" href="${hostname}/share/res/components/falcon/qrcode/browsefolder/browsefolder.css" />
    <link rel="stylesheet" href="${hostname}/share/res/components/falcon/qrcode/browsefolder/oops.css" />
</head>

<style>
</style>

<body>
	<div class="row" style="margin: 0; padding-top: 10px;">
		<div class="col-md-10 col-sm-10">
			<img src="${hostname}/share/res/components/falcon/assets/logo.png">
		</div>
	</div>

	<#if (fileFolders?exists)>

	<div class="row" style="width: 100%; margin: 20px 0px;">
		<div class="col-md-10 col-md-offset-1 col-sm-10 col-sm-offset-1">
			<h4>Path: ${folderPath}</h4>
			<table id="pager">
				<tr><th>Name</th><th>Type</th></tr>
				<#list fileFolders as item>
					<#if item.isContainer>
						<tr>
							<td style="padding: 5px;">
								<a href='${hostname+"/alfresco/s/indesso/falcon/browseFolder?nodeRef="+item.nodeRef}'>${item.name}</a>
							</td>
							<td>Folder</td>
						</tr>
					<#else>
						<tr>
							<td style="padding: 5px;">
								<a href='${hostname+"/alfresco"+item.url+"?ticket="+ticket}' target="_blank">${item.name}</a>
							</td>
							<td>File</td>
						</tr>
					</#if>
				</#list>
			</table>
		</div>
	</div>

	<#else>
		
	<div id="notfound">
		<div class="notfound">
			<div class="notfound-404">
				<h1>Oops!</h1>
			</div>
			<h2>QR Code has been revoked</h2>
			<p>
				<span>The QR Code Access might have been removed.</span>
				<br>
				<span>Please contact administrator.</span>
			</p>
			<#--  <a href="javascript:closePage()"><i class="fas fa-times"></i> Close</a>  -->
			
		</div>
	</div>
	<!-- This templates was made by Colorlib (https://colorlib.com) -->

	</#if>

	<div style="position: fixed; bottom: 0; width: 100%;">
		<div class="col-md-12" style="text-align: center; color: #8888;">
			<div class="copy">&copy; 2005-2016 IT Department. Indesso Group.</div>
		</div>
	</div>
</body>

<script>
	function closePage() {
		window.close()
	}
</script>

</html>