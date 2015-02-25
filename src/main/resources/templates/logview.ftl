<html>
    <head>
        <title>Logfiles</title>
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css"/>
    </head>
    <body>
            <div class="container">
            <div class="panel panel-default">
                <div class="panel-heading">
                    Files in ${loggingPath}
                </div>
                <table class="table table-striped table-hover">
                    <thead>
                        <tr>
                            <th><a href="?sortBy=FILENAME<#if sortBy == 'FILENAME' && desc == false>&desc=true</#if>">Name</a></th>
                            <th><a href="?sortBy=SIZE<#if sortBy == 'SIZE' && desc == false>&desc=true</#if>"">Size</a></th>
                            <th><a href="?sortBy=MODIFIED<#if sortBy == 'MODIFIED' && desc == false>&desc=true</#if>"">Modified</a></th>
                        </tr>
                    </thead>
                    <#list files as file>
                        <tr>
                            <td><a href="view/${file.filename}/">${file.filename}</a></td>
                            <td>${file.size}</td>
                            <td title="${file.modified}">${file.modifiedPretty}</td>
                        </tr>
                    </#list>
                </table>
            </div>
        </div>
    </body>
</html>