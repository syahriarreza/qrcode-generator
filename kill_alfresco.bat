REM Stop Alfresco
set ALFRESCO_SERVICE=alfrescoTomcat
taskkill /F /FI "SERVICES eq %ALFRESCO_SERVICE%"