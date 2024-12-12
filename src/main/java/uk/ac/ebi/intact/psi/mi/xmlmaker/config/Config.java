package uk.ac.ebi.intact.psi.mi.xmlmaker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.intact.psi.mi.xmlmaker.file.processing.ExcelFileReader;
import uk.ac.ebi.intact.psi.mi.xmlmaker.uniprot.mapping.UniprotMapper;

@Configuration
public class Config {

    @Bean
    public ExcelFileReader getExcelFileReader() {
        return new ExcelFileReader();
    }

    @Bean
    public UniprotMapper getUniprotMapper() {
        return new UniprotMapper();
    }
}
