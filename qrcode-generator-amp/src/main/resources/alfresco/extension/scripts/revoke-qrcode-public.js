var PUBLIC_USERNAME = "public";

function main() {
    if (document.hasAspect("qrcodepublic:inUse")) {
        document.removeAspect("qrcodepublic:inUse");
        document.removePermission("Consumer", "ROLE_GUEST");
        document.removePermission("Consumer", PUBLIC_USERNAME);
        document.save();
    }
}

main();