<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>BDB</title>
    <style>
        h1 {
            text-align: center;
            color: pink;
        }

        #d1 {
            font-family: "comic sans ms", serif;

            position: absolute;

            top: 20%;
            left: 50%;
            transform: translateY(-50%) translateX(-50%);

            margin: 20px auto;
            height: 100px;
            width: 450px;
            border: 10px solid pink;
            text-align: center;
        }
        #d2 {
            font-family: "comic sans ms", serif;

            position: absolute;

            top: 40%;
            left: 50%;
            transform: translateY(-50%) translateX(-50%);

            margin: 20px auto;
            height: 100px;
            width: 450px;
            border: 10px solid pink;
            text-align: center;
        }

        #d3 {
            font-family: "comic sans ms", serif;

            position: absolute;

            top: 60%;
            left: 50%;
            transform: translateY(-50%) translateX(-50%);

            margin: 20px auto;
            height: 100px;
            width: 450px;
            border: 10px solid pink;
            text-align: center;
        }
    </style>
</head>
<body>
<h1>The low quality B-Tree database😂</h1>
<form method="post" action="BTreeServlet">
    <div id="d1">key  :<input type="text" name="key"/><br/>
        value:<input type="text" name="value"/><br/>
        <input type="hidden" name="type" value="put">
        <button type="submit">insert</button>
    </div>
</form>

<form method="post" action="BTreeServlet">
    <div id="d2">key:<input type="text" name="key"/><br/>
        <input type="hidden" name="type" value="delete">
        <button type="submit">delete</button>
    </div>
</form>


<form method="get" action="BTreeServlet">
    <div id="d3">key:<input type="text" name="key"/><br/>
        <button type="submit">get</button>
    </div>
</form>
</body>
</html>