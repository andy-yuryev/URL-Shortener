$(document).ready(function() {
	$("button").click(function() {
		$.ajax({
			type : 'PUT',
			url : 'http://localhost:8080/put/',
			data : $("#urlinput").val(),
			contentType : "text/plain; charset=utf-8",
			success : function(data) {
				$("#urlinput").val(data);
			}
		});
	});
});