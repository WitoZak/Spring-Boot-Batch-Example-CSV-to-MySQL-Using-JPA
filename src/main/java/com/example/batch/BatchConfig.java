package com.example.batch;

import lombok.Data;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import static org.springframework.batch.item.file.transform.DelimitedLineTokenizer.DELIMITER_COMMA;

@Data
@Configuration
@EnableBatchProcessing
public class BatchConfig {


    private InvoiceRepository repository;

    private JobBuilderFactory jbf;

    private StepBuilderFactory sbf;

    //Reader class Object

    @Bean
    public LineTokenizer lineTokenizer() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(DELIMITER_COMMA);
        tokenizer.setNames("name", "number", "amount", "discount", "location");
        return tokenizer;
    }

    @Bean
    public FieldSetMapper<Invoice> fieldSetMapper() {
        BeanWrapperFieldSetMapper<Invoice> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(Invoice.class);
        return mapper;
    }

    @Bean
    public LineMapper<Invoice> lineMapper() {
        DefaultLineMapper<Invoice> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(lineTokenizer());
        lineMapper.setFieldSetMapper(fieldSetMapper());
        return lineMapper;
    }

    @Bean
    public FlatFileItemReader<Invoice> reader() {
        FlatFileItemReader<Invoice> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("/invoise.csv"));
        reader.setLineMapper(lineMapper());
        reader.setRecordSeparatorPolicy(new BlankLineRecordSeparatorPolicy());
        return reader;
    }

    //Writer class Object
    @Bean
    public ItemWriter<Invoice> writer() {
        return invoices -> {
            System.out.println("Saving Invoice Records: " + invoices);
            repository.saveAll(invoices);
        };
    }


    //Processor class Object
    @Bean
    public ItemProcessor<Invoice, Invoice> processor() {
        // return new InvoiceProcessor(); // Using lambda expression code instead of a separate implementation
        return invoice -> {
            Double discount = invoice.getAmount() * (invoice.getDiscount() / 100.0);
            Double finalAmount = invoice.getAmount() - discount;
            invoice.setFinalAmount(finalAmount);
            return invoice;
        };
    }

    //Listener class Object
    @Bean
    public JobExecutionListener listener() {
        return new InvoiceListener();
    }


    //Step Object
    @Bean
    public Step stepA() {
        return sbf.get("stepA")
                .<Invoice, Invoice>chunk(2)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();
    }

    @Bean
    public Job jobA() {
        return jbf.get("jobA")
                .incrementer(new RunIdIncrementer())
                .listener(listener())
                .start(stepA())
                .build();
    }

}