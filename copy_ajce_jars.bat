REM AJCE
set ALFRESCO_HOME=D:\AlfrescoCom51
set WEBAPPS_HOME=%ALFRESCO_HOME%\tomcat\webapps

set AJCE_JAR=..\alfresco-java-code-executer\[ Jars ]\ajce-0.2-SNAPSHOT.jar
set WEBAPPS_LIB_HOME=%WEBAPPS_HOME%\alfresco\WEB-INF\lib
REM set AJCE_MVN_JAR=D:\AlfrescoCom51\tomcat\_WORKSPACE_\Git\alfresco-java-code-executer\[ Jars ]\ajce-maven-plugin-0.2-SNAPSHOT.jar

if not exist "%WEBAPPS_LIB_HOME%" (
    echo "NOT EXIIISSTTT"
    mkdir "%WEBAPPS_LIB_HOME%"
)

xcopy /Y "%AJCE_JAR%" "%WEBAPPS_LIB_HOME%"
REM xcopy /Y "%AJCE_MVN_JAR%" "%WEBAPPS_LIB_HOME%"