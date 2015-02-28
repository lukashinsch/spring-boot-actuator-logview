<html>
    <head>
        <title>Logfiles</title>
        <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css"/>
        <link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/font-awesome/4.3.0/css/font-awesome.min.css">
    </head>
    <body>
            <div class="container">
                <div class="page-header">
                    <h1>Log file viewer</h1>
                </div>
            <div class="panel panel-default">
                <div class="panel-heading">
                    Current location ${currentFolder}
                </div>
                <table class="table table-striped table-hover">
                    <thead>
                        <tr>
                            <th><a href="?sortBy=FILENAME<#if sortBy == 'FILENAME' && desc == false>&desc=true</#if>&base=${base}">Name</a></th>
                            <th><a href="?sortBy=SIZE<#if sortBy == 'SIZE' && desc == false>&desc=true</#if>&base=${base}">Size</a></th>
                            <th><a href="?sortBy=MODIFIED<#if sortBy == 'MODIFIED' && desc == false>&desc=true</#if>&base=${base}">Modified</a></th>
                        </tr>
                    </thead>
                    <#if base != "">
                    <tr>
                        <td><i class="fa fa-folder-o"></i>&nbsp;<a href="?base=${parent}">..</a></td>
                        <td></td>
                        <td></td>
                    </tr>
                    </#if>

                    <#list files as file>
                        <tr>
                            <td>
                                <#if file.fileType == 'FILE'>
                                    <i class="fa fa-file-o"></i>
                                </#if>
                                <#if file.fileType == 'DIRECTORY'>
                                    <i class="fa fa-folder-o"></i>
                                </#if>
                                <#if file.fileType == 'ARCHIVE'>
                                    <i class="fa fa-file-archive-o"></i>
                                </#if>
                                &nbsp;
                                <#if file.fileType == 'FILE'>
                                    <a href="view/${file.filename}/?base=${base}">${file.filename}</a>
                                </#if>
                                <#if file.fileType == 'ARCHIVE'>
                                    <a href="?base=${base}/${file.filename}">${file.filename}</a>
                                </#if>
                                <#if file.fileType == 'DIRECTORY'>
                                    <a href="?base=${base}/${file.filename}">${file.filename}</a>
                                </#if>
                            </td>
                            <td>
                                <#if file.fileType != 'DIRECTORY'>
                                    ${file.size}
                                </#if>
                            </td>
                            <td title="${file.modified}">${file.modifiedPretty}</td>
                        </tr>
                    </#list>
                </table>
            </div>
        </div>
    </body>
</html>