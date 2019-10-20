rem ### Script to copy amps and amps share to amps and amps_share folder ###
set ALFRESCO_HOME=D:\AlfrescoCom51\
set WEBAPPS_HOME=D:\AlfrescoCom51\tomcat\webapps

xcopy /Y "qrcode-generator-amp\target\*.amp" "%ALFRESCO_HOME%\amps"
xcopy /Y "qrcode-generator-amp-share\target\*.amp" "%ALFRESCO_HOME%\amps_share"

del /Q "%WEBAPPS_HOME%\alfresco.war"
del /Q "%WEBAPPS_HOME%\share.war"

copy "%WEBAPPS_HOME%\alfresco.war-001.FRESH.bak" "%WEBAPPS_HOME%\alfresco.war"
copy "%WEBAPPS_HOME%\share.war-001.FRESH.bak" "%WEBAPPS_HOME%\share.war"

call "%ALFRESCO_HOME%bin\apply_amps - Force NoWait.bat"