<html>
    <head>
        <title>Logfiles</title>
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css"/>
    </head>
    <body>
            <div class="container">
            <div class="panel panel-default">
                <div class="panel-heading">
                    Files in ${currentFolder}
                </div>
                <table class="table table-striped table-hover">
                    <thead>
                        <tr>
                            <th>Type</th>
                            <th><a href="?sortBy=FILENAME<#if sortBy == 'FILENAME' && desc == false>&desc=true</#if>">Name</a></th>
                            <th><a href="?sortBy=SIZE<#if sortBy == 'SIZE' && desc == false>&desc=true</#if>"">Size</a></th>
                            <th><a href="?sortBy=MODIFIED<#if sortBy == 'MODIFIED' && desc == false>&desc=true</#if>"">Modified</a></th>
                        </tr>
                    </thead>
                    <#if base != "">
                    <tr>
                        <td></td>
                        <td><a href="?base=${parent}">..</a></td>
                        <td></td>
                        <td></td>
                    </tr>
                    </#if>

                    <#list files as file>
                        <tr>
                            <td>${file.fileType}</td>
                            <td>
                                <#if file.fileType != 'DIRECTORY'>
                                    <a href="view/${file.filename}/?base=${base}">${file.filename}</a>
                                </#if>
                                <#if file.fileType == 'DIRECTORY'>
                                    <a href="?base=${base}/${file.filename}">${file.filename}</a>
                                </#if>
                            </td>
                            <td>${file.size}</td>
                            <td title="${file.modified}">${file.modifiedPretty}</td>
                        </tr>
                    </#list>
                </table>
            </div>
        </div>
    </body>
</html>