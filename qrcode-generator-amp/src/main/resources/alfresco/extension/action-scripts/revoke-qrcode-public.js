function main() {
    if (document.hasAspect("qrcodepublic:inUse")) {
        document.removeAspect("qrcodepublic:inUse");
        document.save();
    }
}

main();