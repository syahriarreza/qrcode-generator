REM @echo off
rem ### Script to copy amps and amps share to amps and amps_share folder ###
set ALFRESCO_SERVICE=alfrescoTomcat
set ALFRESCO_HOME=D:\AlfrescoCom51
set WEBAPPS_HOME=%ALFRESCO_HOME%\tomcat\webapps
set APPLY_AMPS_BAT=apply_amps - Force NoWait.bat
set ALF_WAR_ORI=%ALFRESCO_HOME%\tomcat\_WORKSPACE_\backups\alfresco.war-001.FRESH.bak
set SHARE_WAR_ORI=%ALFRESCO_HOME%\tomcat\_WORKSPACE_\backups\share.war-001.FRESH.bak

REM Stop Alfresco
taskkill /F /FI "SERVICES eq %ALFRESCO_SERVICE%"

REM Maven Install (Generate amps)
call mvn clean install -DskipTests -f qrcode-generator-amp
call mvn clean install -DskipTests -f qrcode-generator-amp-share

REM Copy amp
xcopy /Y "qrcode-generator-amp\target\*.amp" "%ALFRESCO_HOME%\amps"
xcopy /Y "qrcode-generator-amp-share\target\*.amp" "%ALFRESCO_HOME%\amps_share"

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