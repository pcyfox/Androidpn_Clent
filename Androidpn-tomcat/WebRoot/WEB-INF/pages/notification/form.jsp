<%@ page language="java" errorPage="/error.jsp" pageEncoding="UTF-8"
	contentType="text/html;charset=UTF-8"%>
<%@ include file="/includes/taglibs.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<title>Admin Console</title>
<meta name="menu" content="notification" />
</head>

<body>

	<h1>消息推送</h1>

	<%--<div style="background:#eee; margin:20px 0px; padding:20px; width:500px; border:solid 1px #999;">--%>
	<div style="margin:20px 0px;">
		<form action="" method="post" style="margin: 0px;">
			<table width="600" cellpadding="4" cellspacing="0" border="0">
				<tr>
					<td width="20%">发送方式:</td>
					<td width="80%"><input type="radio" name="broadcast" value="0"
						checked="checked" /> 所有用户 <input type="radio" name="broadcast"
						value="1" /> 指定用户ID <input type="radio" name="broadcast" value="2" />指定别称
						<input type="radio" name="broadcast" value="3" />指定名称</td>
				</tr>

				<tr id="trUsername" style="display:none;">
					<td>用户ID:</td>
					<td><input type="text" id="username" name="username" value=""
						style="width:380px;" />
					</td>
				</tr>

				<tr id="trAlias" style="display:none;">
					<td>别名:</td>
					<td><input type="text" id="alias" name="alias" value=""
						style="width:380px;" />
					</td>
				</tr>

				<tr id="trRealName" style="display:none;">
					<td>名称:</td>
					<td><input type="text" id="realName" name="realName" value=""
						style="width:380px;" />
					</td>
				</tr>

				<tr>
					<td>标题:</td>
					<td><input type="text" id="title" name="title" value="《江雪》"
						style="width:380px;" />
					</td>
				</tr>
				<tr>
					<td>消息:</td>
					<td><textarea id="message" name="message"
							style="width:380px; height:80px;">千山三鸟飞绝，万径人踪灭。孤舟蓑笠翁，独钓寒江雪。 </textarea>
					</td>
				</tr>
				<%--
<tr>
	<td>Ticker:</td>
	<td><input type="text" id="ticker" name="ticker" value="" style="width:380px;" /></td>
</tr>
--%>
				<tr>
					<td>URI:</td>
					<td><input type="text" id="uri" name="uri" value=""
						style="width:380px;" /> <br /> <span style="font-size:0.8em">ex)
							http://www.dokdocorea.com, geo:37.24,131.86, tel:111-222-3333</span>
					</td>
				</tr>
				<tr>
					<td>&nbsp;</td>
					<td><input id="tijiao" type="button" value="发送" />
					</td>
				</tr>
			</table>
		</form>
	</div>

	<script type="text/javascript">
		//         

		$(function() {
			$('input[name=broadcast]').click(function() {
				if ($('input[name=broadcast]')[0].checked) {
					$('#trUsername').hide();
					$('#trAlias').hide();
					$('#trRealName').hide();
				} else if ($('input[name=broadcast]')[1].checked) {
					$('#trUsername').show();
					$('#trAlias').hide();
					$('#trRealName').hide();
				} else if ($('input[name=broadcast]')[2].checked) {
					$('#trUsername').hide();
					$('#trAlias').show();
					$('#trRealName').hide();
				} else if ($('input[name=broadcast]')[3].checked) {
					$('#trUsername').hide();
					$('#trAlias').hide();
					$('#trRealName').show();
				}

			});

			
		});

		var broadcast = "";
		function getCheckedResult() {
			var radios = document.getElementsByName("broadcast");
			for ( var i = 0, length = radios.length; i < length; i++) {

				if (radios[i].checked) {
					broadcast = i;
				}
			}

		}

		$("#tijiao").click(function() {
			getCheckedResult();

			$.post("notification.do?action=send", {
				"broadcast" : broadcast,
				"username" : $("#username").val(),
				"realName" : $("#realName").val(),
				"alias" : $("#alias").val(),
				"title" : $("#title").val(),
				"message" : $("#message").val()
			});
		});

		//
	</script>

</body>
</html>
