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
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;

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

    //factorisation des champs communs aux fonction processEmploye, processManager, processCommercial
    public void processEmploye(String[] fields, Employe emp) throws BatchException{


        //
        if (!fields[0].matches(REGEX_MATRICULE)){
            throw new BatchException("La chaîne " + fields[0] + " ne respecte pas l'expression régulière " + REGEX_MATRICULE);
        }
        if (!fields[1].matches(REGEX_NOM)){
            throw new BatchException(fields[1] + " n'est pas un nom valide ");
        }
        if (!fields[2].matches(REGEX_PRENOM)){
            throw new BatchException(fields[2] + " n'est pas un prénom valide ");
        }

        LocalDate date;
        try {
            date = (DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(fields[3]));
        }catch(Exception e){
            throw new BatchException(fields[3] + " ne respecte pas le format de date dd/MM/yyyy");
        }
        Double salaire;
        try {
            salaire = Double.parseDouble(fields[4]);
        }
        catch (Exception e){
            throw new BatchException(fields[4] + " n'est pas un nombre valide pour un salaire " );
        }


        emp.setMatricule(fields[0]);
        emp.setNom(fields[1]);
        emp.setPrenom(fields[2]);
        emp.setDateEmbauche(date);
        emp.setSalaire(salaire);
    }
    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        //Contrôle la taille de la ligne rentrée

        String[] commercialFields = ligneCommercial.split(",");
        Commercial c = new Commercial();
        if (commercialFields.length != NB_CHAMPS_COMMERCIAL){
            throw new BatchException("La ligne manager ne contient pas " + NB_CHAMPS_COMMERCIAL + " éléments mais " + commercialFields.length + "  => " + ligneCommercial);
        }

        processEmploye(commercialFields, c);
        //contrôle du CA
        Double ca;
        try {
            ca = Double.parseDouble(commercialFields[5]);
        } catch (Exception e) {
            throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + commercialFields[5] + " ");
        }
        //Controle de l'indice de performance.0
        Integer perf;
        try {
            perf = Integer.parseInt(commercialFields[6]);
        }
        catch (Exception e){
            throw new BatchException("La performance du commercial est incorrecte : " + commercialFields[6] + " ");
        }

        //création du commercial
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
        //Vérification du nombre de champs dans un manager
        String[] managerField = ligneManager.split(",");
        if (managerField.length != NB_CHAMPS_MANAGER){
            throw new BatchException("La ligne manager ne contient pas " + NB_CHAMPS_MANAGER + " éléments mais " + managerField.length + " ");
        }

        //création du manager
        Manager m= new Manager();
        processEmploye(managerField , m);
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

        //Vérification du nombre de champs pour le technicien
        if (technicienField.length != NB_CHAMPS_TECHNICIEN){
            throw new BatchException("La ligne technicien ne contient pas " + NB_CHAMPS_TECHNICIEN + " éléments mais " + technicienField.length + "  ");
        }
        Technicien t = new Technicien();


        //Vérification du grade
        Integer grade;
        try {
            grade = Integer.parseInt(technicienField[5]);
        }
        catch (Exception e){
            throw new BatchException("Le grade du technicien est incorrect : " + technicienField[5] + " ");
        }
        //Vérification du manager du technicien
        Manager manager;
        manager = null;
        if (!technicienField[6].matches(REGEX_MATRICULE_MANAGER)){
            throw new BatchException("Le manager de matricule " + technicienField[6] + " n'a pas été trouvé dans le fichier ou en base de données ");
            // RAF Gérer les M00001 qui ont été remplacé par M87654 pour avancer sur le reste
        }

        for (int i = 0;i < employes.size();i++) {
            if (employes.get(i) instanceof Manager && employes.get(i).getMatricule().equals(technicienField[6])){
                manager = (Manager) employes.get(i);
            }
        }
        if (manager == null) {
            throw new BatchException("Le manager de matricule " + technicienField[6] + " n'a pas été trouvé dans le fichier ou en base de données ");
        }

        //Vérification que le grade est bien compris entre 1 et 5
        try {
            t.setGrade(grade);
        } catch (TechnicienException e) {
            throw new BatchException("Le grade doit être compris entre 1 et 5 : " + technicienField[5] + " ");
        }
        processEmploye(technicienField , t);
        t.setManager(manager);
        employes.add(t);
    }

}
