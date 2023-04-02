package edu.uclm.esi.gamesusers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.*;

@SpringBootApplication
@ServletComponentScan
public class LanzadoraGames 
{
    public static void main( String[] args )
    {
        SpringApplication.run(LanzadoraGames.class, args);
    }
}
