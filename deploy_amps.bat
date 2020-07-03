REM @echo off
rem ### Script to copy amps and amps share to amps and amps_share folder ###
set ALFRESCO_SERVICE=alfrescoTomcat
set ALFRESCO_HOME=D:\AlfrescoCom51
set WEBAPPS_HOME=%ALFRESCO_HOME%\tomcat\webapps
set WEBAPPS_LIB_HOME=%WEBAPPS_HOME%\alfresco\WEB-INF\lib
set APPLY_AMPS_BAT=apply_amps - Force NoWait.bat

set AJCE_FOLDER=..\alfresco-java-code-executer\platform-jar
set AMP_FOLDER=qrcode-generator-amp
set AMP_SHARE_FOLDER=qrcode-generator-amp-share

set ALF_WAR_ORI=%ALFRESCO_HOME%\tomcat\_WORKSPACE_\backups\war\alfresco.war-001.FRESH.bak
set SHARE_WAR_ORI=%ALFRESCO_HOME%\tomcat\_WORKSPACE_\backups\war\share.war-001.FRESH.bak


REM Stop Alfresco
taskkill /F /FI "SERVICES eq %ALFRESCO_SERVICE%"

REM Maven Install (Generate amps)
call mvn clean install -DskipTests -f %AJCE_FOLDER%
call mvn clean install -DskipTests -f %AMP_FOLDER%
call mvn clean install -DskipTests -f %AMP_SHARE_FOLDER%
call mvn clean install -DskipTests -f "D:\AlfrescoCom51\tomcat\_WORKSPACE_\Git\falcon-assets-amp-share"

REM Copy amp
xcopy /Y "%AJCE_FOLDER%\target\*.amp" "%ALFRESCO_HOME%\amps"
xcopy /Y "%AMP_FOLDER%\target\*.amp" "%ALFRESCO_HOME%\amps"
xcopy /Y "%AMP_SHARE_FOLDER%\target\*.amp" "%ALFRESCO_HOME%\amps_share"
xcopy /Y "D:\AlfrescoCom51\tomcat\_WORKSPACE_\Git\falcon-assets-amp-share\target\*.amp" "%ALFRESCO_HOME%\amps_share"

REM Refresh alfresco.war and share.war
del /Q "%WEBAPPS_HOME%\alfresco.war"
del /Q "%WEBAPPS_HOME%\share.war"
copy "%ALF_WAR_ORI%" "%WEBAPPS_HOME%\alfresco.war"
copy "%SHARE_WAR_ORI%" "%WEBAPPS_HOME%\share.war"

REM Apply amps
call "%ALFRESCO_HOME%\bin\%APPLY_AMPS_BAT%"

REM Clean up war files
del /Q "%WEBAPPS_HOME%\alfresco.war-*"
del /Q "%WEBAPPS_HOME%\share.war-*"

REM Start Alfresco
sc start %ALFRESCO_SERVICE%

REM Print Date
for /f "tokens=2 delims==" %%a in ('wmic OS Get localdatetime /value') do set "dt=%%a"
set "YY=%dt:~2,2%" & set "YYYY=%dt:~0,4%" & set "MM=%dt:~4,2%" & set "DD=%dt:~6,2%"
set "HH=%dt:~8,2%" & set "Min=%dt:~10,2%" & set "Sec=%dt:~12,2%"
set "fullstamp=%YYYY%-%MM%-%DD% %HH%:%Min%:%Sec%"

echo DEPLOYMENT FINISHED at: %fullstamp%