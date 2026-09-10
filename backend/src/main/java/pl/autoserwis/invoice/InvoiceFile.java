package pl.autoserwis.invoice;

import java.util.regex.Pattern;

public record InvoiceFile(String filename, byte[] content) {
    private static final Pattern UNSAFE_FILENAME = Pattern.compile("[^A-Za-z0-9._-]");

    public InvoiceFile {
        filename = UNSAFE_FILENAME.matcher(filename).replaceAll("-");
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
