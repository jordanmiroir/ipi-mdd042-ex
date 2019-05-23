package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = "^[\\p{L}- ]*$";
    private static final String REGEX_PRENOM = "^[\\p{L}- ]*$";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;
    private static final String REGEX_SALAIRE = "[0-9]*.[0-9]";
    private static final String REGEX_INT = "[0-9]*";
    private static final String REGEX_GRADE_OK = "[1-5]";

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception {
        String fileName = "employes.csv";
        readFile(fileName);
        //readFile(strings[0]);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName) {
        Stream<String> stream;
        logger.info("Lecture du fichier : "+fileName);

        try{
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        }catch(IOException e){
            logger.error("Problème dans l'ouverture du fichier "+fileName);
            return new ArrayList<>();
        }

        List<String>lignes = stream.collect(Collectors.toList());
        logger.info(lignes.size()+"lignes lues");

        for(int i=0; i<lignes.size();i++){
            try {
                processLine(lignes.get(i));
            } catch (BatchException e) {

                logger.error("Ligne"+(i+1)+" : "+e.getMessage()+" => " + lignes.get(i));

            }
        }
        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
        //TODO
        switch(ligne.substring(0,1)){
            case "T":
                processTechnicien(ligne);
                break;
            case "M":
                processManager(ligne);
                break;
            case "C":
                processCommercial(ligne);
                break;
            default:
                throw new BatchException(" : Type d'employé inconnu : ");
        }

    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        //TODO


        String[] commercialFields = ligneCommercial.split(",");
        LocalDate date;


        if(!(commercialFields.length==NB_CHAMPS_COMMERCIAL)){
            throw new BatchException("Le commercial ne comprend pas le bon nombre de champs !");
        }
        if(!commercialFields[0].matches(REGEX_MATRICULE)) {
            throw new BatchException(": la chaîne Cxxxxx ne respecte pas l'expression régulière ^[MTC][0-9]{5}$ ");
        }
        if(!commercialFields[1].matches(REGEX_NOM)){
            throw new BatchException(": la chaîne NOM ne respecte pas l'expression régulière ^[\\p{L}- ]*$ ");
        }
        if(!commercialFields[2].matches(REGEX_PRENOM)) {
            throw new BatchException(": la chaîne PRENOM ne respecte pas l'expression régulière ^[\\p{L}- ]*$ ");
        }
        try{
            date = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(commercialFields[3]);
        } catch (Exception e){
            throw new BatchException(": la date n'est pas a un format valide ");
        }
        if(!commercialFields[4].matches(REGEX_SALAIRE)) {
            throw new BatchException(": la chaîne SALAIRE ne respecte pas l'expression régulière [0-9]*.[0-9] ");
        }
        Double salaire = Double.parseDouble(commercialFields[4]);

        if(!commercialFields[5].matches(REGEX_INT)){
            throw new BatchException(": Le chiffre d'affaire du commercial est incorrecte : "+commercialFields[5]);
        }
        Double ca = Double.parseDouble(commercialFields[5]);
        if(!commercialFields[6].matches(REGEX_INT)){
            throw new BatchException(" : L'indice de performance n'est pas bon");
        }
        Integer performance = Integer.parseInt(commercialFields[6]);

        Commercial c = new Commercial();
        c.setMatricule(commercialFields[0]);
        c.setNom(commercialFields[1]);
        c.setPrenom(commercialFields[2]);
        c.setDateEmbauche(date);
        c.setSalaire(salaire);
        c.setPerformance(performance);
        c.setCaAnnuel(ca);
        employes.add(c);

    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        //TODO
        String[] managerFields = ligneManager.split(",");
        LocalDate date;


        if(!(managerFields.length==NB_CHAMPS_MANAGER)){
            throw new BatchException("Le commercial ne comprend pas le bon nombre de champs !");
        }
        if(!managerFields[0].matches(REGEX_MATRICULE)) {
            throw new BatchException(": la chaîne Mxxxxx ne respecte pas l'expression régulière ^[MTC][0-9]{5}$ ");
        }
        if(!managerFields[1].matches(REGEX_NOM)){
            throw new BatchException(": la chaîne avec le NOM ne respecte pas l'expression régulière ^[\\p{L}- ]*$ ");
        }
        if(!managerFields[2].matches(REGEX_PRENOM)) {
            throw new BatchException(": la chaîne avec le PRENOM ne respecte pas l'expression régulière ^[\\p{L}- ]*$ ");
        }
        try{
            date = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(managerFields[3]);
        } catch (Exception e){
            throw new BatchException(": La date n'est pas a un format valide ");
        }
        if(!managerFields[4].matches(REGEX_SALAIRE)) {
            throw new BatchException(": La chaîne SALAIRE ne respecte pas l'expression régulière [0-9]*.[0-9] ");
        }
        Double salaire = Double.parseDouble(managerFields[4]);


        Manager m = new Manager();
        m.setMatricule(managerFields[0]);
        m.setNom(managerFields[1]);
        m.setPrenom(managerFields[2]);
        m.setDateEmbauche(date);
        m.setSalaire(salaire);
        employes.add(m);


    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        //TODO
        String[] technicienFields = ligneTechnicien.split(",");
        LocalDate date;

        if(!(technicienFields.length==NB_CHAMPS_TECHNICIEN)){
            throw new BatchException("Le commercial ne comprend pas le bon nombre de champs !");
        }
        if(!technicienFields[0].matches(REGEX_MATRICULE)) {
            throw new BatchException(": La chaîne Txxxxx ne respecte pas l'expression régulière ^[MTC][0-9]{5}$ ");
        }
        if(!technicienFields[1].matches(REGEX_NOM)){
            throw new BatchException(": La chaîne NOM ne respecte pas l'expression régulière ^[\\p{L}- ]*$ ");
        }
        if(!technicienFields[2].matches(REGEX_PRENOM)) {
            throw new BatchException(": La chaîne PRENOM ne respecte pas l'expression régulière ^[\\p{L}- ]*$ ");
        }
        try{
            date = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(technicienFields[3]);
        } catch (Exception e){
            throw new BatchException(": La date n'est pas a un format valide ");
        }
        if(!technicienFields[4].matches(REGEX_SALAIRE)) {
            throw new BatchException(": la chaîne SALAIRE du technicien ne respecte pas l'expression régulière [0-9]*.[0-9] ");
        }
        Double salaire = Double.parseDouble(technicienFields[4]);
        if(!technicienFields[5].matches(REGEX_INT)){
            throw new BatchException(": Le grade du technicien est incorrecte : "+technicienFields[5]);
        }
        if(!technicienFields[5].matches(REGEX_GRADE_OK)){
            throw new BatchException(" : Le grade n'est pas compris entre 1 et 5 !");
        }
        Integer grade = Integer.parseInt(technicienFields[5]);
        if(!technicienFields[6].matches(REGEX_MATRICULE_MANAGER)){
            throw new BatchException("Le technicien n'a pas de manager");
        }

        Technicien t = new Technicien();
        t.setMatricule(technicienFields[0]);
        t.setNom(technicienFields[1]);
        t.setPrenom(technicienFields[2]);
        t.setDateEmbauche(date);
        t.setSalaire(salaire);
        t.setGrade(grade);
        t.setManager(technicienFields[6]);
        employes.add(t);

    }

}
