<html>

<head>
	<title>Document List</title>
	<link rel="stylesheet" href="${url.context}/css/main.css" TYPE="text/css">
</head>

<style>
	body {
		background-color: #fff;
		color: #24292e;
		font-family: "Poppins", -apple-system, BlinkMacSystemFont, Segoe UI, Helvetica, Arial, sans-serif;
		font-size: 14px;
		line-height: 1.5;
	}

	.pager-nav {
		margin: 16px 0;
	}
	.pager-nav span {
		display: inline-block;
		padding: 4px 8px;
		margin: 1px;
		cursor: pointer;
		font-size: 14px;
		background-color: #FFFFFF;
		border: 1px solid #e1e1e1;
		border-radius: 3px;
		box-shadow: 0 1px 1px rgba(0,0,0,.04);
	}
	.pager-nav span:hover,
	.pager-nav .pg-selected {
		color: white;
		background-color: #2a2afb;
		border: 1px solid #CCCCCC;
	}
</style>

<body>
	<h3>Documents in ${folderPath}</h3>
	<p>
	<table id="pager" class="wp-list-table widefat striped posts" border="1">
		<tr><th>Name</th><th>Type</th></tr>
		<#if (fileFolders?exists && fileFolders?size > 0)>
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
		<#else>
			<tr>
				<td style="padding: 5px;" colspan="2">
					<span style="color: red;">This QR Code access was removed !</span>
				</td>
			</tr>
		</#if>
	</table>

	<div id="pageNavPosition" class="pager-nav"></div>
</body>

<script>
	//--Table Pagination from PagerJS.js
	function Pager(tableName, itemsPerPage) {
		'use strict';

		this.tableName = tableName;
		this.itemsPerPage = itemsPerPage;
		this.currentPage = 1;
		this.pages = 0;
		this.inited = false;

		this.showRecords = function (from, to) {
			let rows = document.getElementById(tableName).rows;

			// i starts from 1 to skip table header row
			for (let i = 1; i < rows.length; i++) {
				if (i < from || i > to) {
					rows[i].style.display = 'none';
				} else {
					rows[i].style.display = '';
				}
			}
		};

		this.showPage = function (pageNumber) {
			if (!this.inited) {
				// Not initialized
				return;
			}

			let oldPageAnchor = document.getElementById('pg' + this.currentPage);
			oldPageAnchor.className = 'pg-normal';

			this.currentPage = pageNumber;
			let newPageAnchor = document.getElementById('pg' + this.currentPage);
			newPageAnchor.className = 'pg-selected';

			let from = (pageNumber - 1) * itemsPerPage + 1;
			let to = from + itemsPerPage - 1;
			this.showRecords(from, to);

			let pgNext = document.querySelector('.pg-next'),
				pgPrev = document.querySelector('.pg-prev');

			if (this.currentPage == this.pages) {
				pgNext.style.display = 'none';
			} else {
				pgNext.style.display = '';
			}

			if (this.currentPage === 1) {
				pgPrev.style.display = 'none';
			} else {
				pgPrev.style.display = '';
			}
		};

		this.prev = function () {
			if (this.currentPage > 1) {
				this.showPage(this.currentPage - 1);
			}
		};

		this.next = function () {
			if (this.currentPage < this.pages) {
				this.showPage(this.currentPage + 1);
			}
		};

		this.init = function () {
			let rows = document.getElementById(tableName).rows;
			let records = (rows.length - 1);

			this.pages = Math.ceil(records / itemsPerPage);
			this.inited = true;
		};

		this.showPageNav = function (pagerName, positionId) {
			if (!this.inited) {
				// Not initialized
				return;
			}

			let element = document.getElementById(positionId),
				pagerHtml = '<span onclick="' + pagerName + '.prev();" class="pg-normal pg-prev">&#171;</span>';

			for (let page = 1; page <= this.pages; page++) {
				pagerHtml += '<span id="pg' + page + '" class="pg-normal pg-next" onclick="' + pagerName + '.showPage(' + page + ');">' + page + '</span>';
			}

			pagerHtml += '<span onclick="' + pagerName + '.next();" class="pg-normal">&#187;</span>';

			element.innerHTML = pagerHtml;
		};
	}

	let pager = new Pager('pager', 10);

	pager.init();
	pager.showPageNav('pager', 'pageNavPosition');
	pager.showPage(1);
</script>

</html>