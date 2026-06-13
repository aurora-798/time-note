package com.note;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.note.mapper")
public class TimeNoteBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TimeNoteBackendApplication.class, args);
    }

}
