<%@ page language="java" contentType="text/html; charset=utf8"
    pageEncoding="utf8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf8">
<link rel="stylesheet" href="${basePath}/static/css/user.css">
<script type="text/javascript" src="${basePath}/static/js/jquery-3.2.1.min.js"></script>
<style type="text/css" >
    .header ul li {
        list-style: none;
        display: inline-block;*display:inline;*zoom:1;
    }
</style>
<title>用户首页</title>
</head>
<body>

<div class="header">
	<ul>
	    <li class=""><a href= "${basePath}/user/index.do">用户首页</a></li>
	    <li><a href= "${basePath}/user/addUser.do">添加用户</a></li>
	</ul>
</div>