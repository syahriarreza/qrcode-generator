function main() {
    if (document.hasAspect("qrcodepublic:inUse")) {
        document.removeAspect("qrcodepublic:inUse");
        document.removePermission("Consumer", "ROLE_GUEST");
        document.save();
    }
}

main();