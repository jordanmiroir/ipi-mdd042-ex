package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.exceptions.TechnicienException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
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
    private static final String REGEX_SALAIRE = "[0-9]*.[0-9]";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;
    private static final String REGEX_PERF = "[0-9]*";
    private static final String REGEX_CA = "[0-9]*";
    private static final String REGEX_GRADE = "[0-9]*";

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
    public List<Employe> readFile(String fileName){
        Stream<String> stream;
        logger.info("lecture du fichier : " + fileName);
        try{
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        }catch (IOException e){
            logger.error("problème dans l'ouverture du fichier" + fileName);
            return new ArrayList<>();
        }

        List<String> lignes = stream.collect(Collectors.toList());
        logger.info(lignes.size()+"lignes lues");

        for(int i = 0; i < lignes.size(); i++){
            try {
                processLine(lignes.get(i));
            } catch (BatchException e) {
                //? afficher le logger error
                logger.error("Ligne " + (i+1) + " : " + e.getMessage() + " => " + lignes.get(i));
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
        switch (ligne.substring(0,1)){
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
            throw new BatchException("Type d'employé inconnu : " + ligne);
        }
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        //Contrôle la taille de la ligne rentrée

        String[] commercialFields = ligneCommercial.split(",");
        if (commercialFields.length != 7){
            throw new BatchException("la ligne commercial ne contient pas 7 éléments mais " + commercialFields.length);
        }

        //Contrôle le matricule

        if (!commercialFields[0].matches(REGEX_MATRICULE)){
            throw new BatchException("la chaîne C12 ne respecte pas l'expression régulière");
        }
        if (!commercialFields[1].matches(REGEX_NOM)){
            throw new BatchException("la chaîne de caractère n'est pas un nom");
        }
        if (!commercialFields[2].matches(REGEX_PRENOM)){
            throw new BatchException("la chaîne de caractère n'est pas un prénom");
        }

        LocalDate date;
        try {
            date = (DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(commercialFields[3]));
        }catch(Exception e){
            throw new BatchException("Le format de date est incorrect");
        }
        Double salaire;
        try {
            salaire = Double.parseDouble(commercialFields[4]);
        }
        catch (Exception e){
            throw new BatchException("Le salaire indiqué n'est pas valide");
        }
        Double ca;
        try {
            ca = Double.parseDouble(commercialFields[5]);
        }
        catch (Exception e){
            throw new BatchException("le chiffre d'affaire du commercial est incorect");
        }
        if (!commercialFields[5].matches(REGEX_CA)){
            throw new BatchException("le chiffre représentant le chiffre d'affaire n'est pas valide ");
        }
        Integer perf;
        try {
            perf = Integer.parseInt(commercialFields[6]);
        }
        catch (Exception e){
            throw new BatchException("La perf indiqué n'est pas un chiffre");
        }
        if (!commercialFields[6].matches(REGEX_PERF)){
            throw new BatchException("le chiffre représentant la perf n'est pas valide ");
        }

        Commercial c = new Commercial();
        c.setMatricule(commercialFields[0]);
        c.setNom(commercialFields[1]);
        c.setPrenom(commercialFields[2]);
        c.setDateEmbauche(date);
        c.setSalaire(salaire);
        c.setCaAnnuel(ca);
        c.setPerformance(perf);
        employes.add(c);


    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        //TODO
        String[] managerField = ligneManager.split(",");
        if (managerField.length != 5){
            throw new BatchException("La ligne manager ne contient pas 5 éléments mais " + managerField.length);
        }
        //Contrôle le matricule
        if (!managerField[0].matches(REGEX_MATRICULE)){
            throw new BatchException("la chaîne C12 ne respecte pas l'expression régulière");
        }
        if (!managerField[1].matches(REGEX_NOM)){
            throw new BatchException("la chaîne de caractère n'est pas un nom");
        }
        if (!managerField[2].matches(REGEX_PRENOM)){
            throw new BatchException("la chaîne de caractère n'est pas un prénom");
        }

        Double salaire;
        try {
            salaire = Double.parseDouble(managerField[4]);
        }
        catch (Exception e){
            throw new BatchException("Le salaire indiqué n'est pas valide");
        }

        if (!managerField[4].matches(REGEX_SALAIRE)){
            throw new BatchException("Le salaire indiqué n'est pas valide");
        }

        LocalDate date;
        try {
            date = (DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(managerField[3]));
        }catch(Exception e){
            throw new BatchException("Le format de date est incorrect");
        }

        Manager m= new Manager();
        m.setMatricule(managerField[0]);
        m.setNom(managerField[0]);
        m.setPrenom(managerField[0]);
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
        String[] technicienField = ligneTechnicien.split(",");

        if (technicienField.length != 7){
            throw new BatchException("la ligne commercial ne contient pas 7 éléments mais " + technicienField.length);
        }
        //Contrôle le matricule
        if (!technicienField[0].matches(REGEX_MATRICULE)){
            throw new BatchException("la chaîne C12 ne respecte pas l'expression régulière");
        }
        if (!technicienField[1].matches(REGEX_NOM)){
            throw new BatchException("la chaîne de caractère n'est pas un nom");
        }
        if (!technicienField[2].matches(REGEX_PRENOM)){
            throw new BatchException("la chaîne de caractère n'est pas un prénom");
        }
        try {
            Integer.parseInt(technicienField[5]);
        }catch (Exception e){
            throw new BatchException("le grade du technicien est incorrect");
        }


        LocalDate date;
        try {
            date = (DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(technicienField[3]));
        }catch(Exception e){
            throw new BatchException("Le format de date est incorrect");
        }

        Double salaire;
        try {
            salaire = Double.parseDouble(technicienField[4]);
        }
        catch (Exception e){
            throw new BatchException("Le salaire indiqué n'est pas valide");
        }

        if (!technicienField[4].matches(REGEX_SALAIRE)){
            throw new BatchException("Le salaire indiqué n'est pas valide");
        }

        Integer grade;
        try {
            grade = Integer.parseInt(technicienField[5]);
        }
        catch (Exception e){
            throw new BatchException("le grade est incorrect");
        }

        Manager manager;
        manager = null;
        if (!technicienField[6].matches(REGEX_MATRICULE_MANAGER)){
            throw new BatchException("la chaine " + technicienField[6] + " ne respecte pas l'expression régulière ");
        }

        for (int i = 0;i < employes.size();i++) {
            if (employes.get(i) instanceof Manager && employes.get(i).getMatricule().equals(technicienField[6])){
                manager = (Manager) employes.get(i);
            }
        }
        if (manager == null) {
            throw new BatchException(": Le technicien n'a pas de manager");
        }


        Technicien t = new Technicien();
        t.setMatricule(technicienField[0]);
        t.setNom(technicienField[1]);
        t.setPrenom(technicienField[2]);
        t.setDateEmbauche(date);

        try {
            t.setGrade(grade);
        } catch (TechnicienException e) {
            throw new BatchException("le grade doit être un chiffre");
        }
        t.setSalaire(salaire);
        t.setManager(manager);
        employes.add(t);
    }

}
