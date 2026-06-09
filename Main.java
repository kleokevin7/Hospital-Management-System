import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    // --- Configuration Variables ---
    static final String DB_URL = "jdbc:sqlite:hospital.db";
    static final String GEMINI_API_KEY = "AQ.Ab8RN6KLbjzy6i-xKGU-Rlkt5Y9RJ61VB2n47NhXM9v-SESqpw";
    // -------------------------------

    static final int docUsernameIndex = 6;
    static final int docPasswordIndex = 7;
    static final int receptionistUsernameIndex = 2;
    static final int receptionistPasswordIndex = 3;
    static int selectedPatientIndex = -1;
    static int loggedInDoctorIndex = -1;
    
    // Memory Cache Lists (Synced with SQLite)
    static ArrayList<String[]> doctorsList = new ArrayList<>();
    static ArrayList<String[]> receptionistsList = new ArrayList<>();
    static ArrayList<String[]> wardsList = new ArrayList<>();
    static ArrayList<String[]> patientsList = new ArrayList<>();
    static ArrayList<String[]> diagnosisList = new ArrayList<>();
    static ArrayList<String[]> appointmentsList = new ArrayList<>();
    static ArrayList<String[]> wardPatientSubmissionList = new ArrayList<>();
    
    static ArrayList<String> mainMenuOptions = new ArrayList<>(Arrays.asList(
            "Main Menu", "Admin Section", "Doctor's Section", "Receptionist's Section", "Medical Assistant Bot (Gemini)"
    ));
    
    // [numberOfDoctors, numberOfReceptionists, numberOfWards, numberOfPatients, numberOfDiagnosis, numberOfAppointments, numberOfSubmissions]
    static int[] numberOfEntitiesArray = new int[7];
    static Scanner scanner = new Scanner(System.in);
    static String adminPassword = "";
    static String adminUsername = "";

    // Method to display center-aligned text
    public static void displayCenterAlignedText(String text, int lineWidth) {
        int totalPadding = lineWidth - text.length();
        int leftPadding = totalPadding / 2;
        int rightPadding = totalPadding - leftPadding;
        System.out.printf("%s%s%s\n", "=".repeat(leftPadding), text, "=".repeat(rightPadding));
    }

    public static void displayOptions(ArrayList<String> menu) {
        int lineWidth = 100;
        int sidesLength = lineWidth / 10;
        int optionSpace = (lineWidth - 2 * sidesLength) / 2;
        displayCenterAlignedText(menu.get(0), lineWidth);

        int totalOptions = menu.size();
        for (int i = 1; i < totalOptions; i++) {
            String firstOption = (i) + ". " + menu.get(i++);
            String secondOption = "";
            if (i != totalOptions) secondOption = (i) + ". " + menu.get(i);

            System.out.printf("| %-" + (sidesLength) + "s %-" + optionSpace + "s%-" + optionSpace + "s | %-" + (sidesLength) + "s\n",
                    "| ", firstOption, secondOption, "| ");
        }
        displayCenterAlignedText("0. Go back", lineWidth);
    }

    public static void navigateMenu(ArrayList<String> menu) {
        int choice;
        while (!menu.isEmpty()) {
            displayOptions(menu);
            System.out.print("Enter your choice: ");
            while (!scanner.hasNextInt()) {
                scanner.nextLine();
                System.out.println("Please Enter only numbers");
                System.out.print("Enter your choice: ");
            }
            choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 0) {
                System.out.println("Going back to the previous menu...");
                return;
            } else if (choice > 0 && choice < menu.size()) {
                String selectedOption = menu.get(choice);
                if (callFunction(selectedOption)) {
                    ArrayList<String> options = getSubMenu(selectedOption);
                    navigateMenu(options);
                }
            } else {
                System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    public static ArrayList<String> getSubMenu(String menuName) {
        ArrayList<String> subMenu = new ArrayList<>();
        switch (menuName) {
            case "Main Menu":
                subMenu.addAll(mainMenuOptions);
                break;
            case "Admin Section":
                subMenu.addAll(Arrays.asList("Admin Section", "Register New Doctor", "Edit Existing Doctor", "Get Doctor Details", "Remove Doctor", "Display All Doctors", "Register New Receptionist", "Remove Receptionist", "Edit Receptionist", "Display All Receptionist", "Add Ward", "Edit Ward", "Get Ward Details", "Remove Ward", "Display All Wards"));
                break;
            case "Doctor's Section":
                subMenu.addAll(Arrays.asList("Doctor's Section", "Handle Patient", "Check Upcoming Appointments"));
                break;
            case "Receptionist's Section":
                subMenu.addAll(Arrays.asList("Receptionist's Section", "Add Patient", "Get Patient Details", "Edit Patient Details", "Admit Patient To Ward", "Checkout Patient From Ward", "Get Ward Details", "Create Appointment", "Mark Appointnment As Done", "Check Doctor's Availability", "Get All Doctors"));
                break;
            case "Handle Patient":
                subMenu.addAll(Arrays.asList("Handle Patient", "Get Patient History", "Add Diagnosis"));
                break;
        }
        return subMenu;
    }

    public static boolean callFunction(String subMenu) {
        switch (subMenu) {
            case "Admin Section": return verifyLoginDetails(1);
            case "Doctor's Section": return verifyLoginDetails(2);
            case "Receptionist's Section": return verifyLoginDetails(3);
            case "Medical Assistant Bot (Gemini)": chatWithMedicalBot(); break;
            case "Register New Doctor": registerNewDoctor(); break;
            case "Edit Existing Doctor": handleEditDoctorDetailsMenu(); break;
            case "Get Doctor Details": handleGetDoctorDetails(); break;
            case "Remove Doctor": handleRemoveDoctor(); break;
            case "Display All Doctors": handleDisplayAllDoctors(); break;
            case "Register New Receptionist": handleRegisterNewReceptionist(); break;
            case "Edit Receptionist": handleEditReceptionistDetailsMenu(); break;
            case "Remove Receptionist": handleRemoveReceptionist(); break;
            case "Display All Receptionist": displayAllReceptionists(); break;
            case "Add Ward": addWard(); break;
            case "Edit Ward": handleEditWardDetails(); break;
            case "Get Ward Details": handleGetWardDetails(); break;
            case "Remove Ward": handleRemoveWard(); break;
            case "Display All Wards": displayAllWards(); break;
            case "Handle Patient": return handlePatient();
            case "Get Patient History": getPatientHistory(); break;
            case "Add Diagnosis": addDiagnosis(); break;
            case "Check Upcoming Appointments": checkUpcomingAppointments(); break;
            case "Add Patient": addPatient(); break;
            case "Get Patient Details": handleGetPatientDetails(); break;
            case "Edit Patient Details": handleEditPatientDetailsMenu(); break;
            case "Admit Patient To Ward": handleAdmitPatientToWard(); break;
            case "Create Appointment": handleCreateAppointment(); break;
            case "Mark Appointnment As Done": handleAppointmentStatus(); break;
            case "Check Doctor's Availability": handlecheckDoctorsAvailability(); break;
            case "Get All Doctors": getAllDoctors(); break;
            case "Checkout Patient From Ward": handleCheckoutPatient(); break;
            default: return false;
        }
        return false;
    }

    // --- GEMINI API INTEGRATION ---
    public static void chatWithMedicalBot() {
        System.out.println("\n--- Medical Assistant Bot (Powered by Gemini) ---");
        System.out.println("Type 'exit' to return to the Main Menu.");
        
        HttpClient client = HttpClient.newHttpClient();
        
        while (true) {
            System.out.print("\nYou: ");
            String prompt = scanner.nextLine().trim();
            if (prompt.equalsIgnoreCase("exit")) break;
            if (prompt.isEmpty()) continue;
            
            // Medical context prompt wrapper to keep bot grounded
            String contextPrompt = "You are a helpful and professional Medical Assistant Bot. " +
                                   "Answer this medical-related query concisely: " + prompt;

            try {
                String escapedPrompt = contextPrompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
                String jsonPayload = "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}]}";
                
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + GEMINI_API_KEY))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                System.out.println("Thinking...");
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                String responseBody = response.body();
                int statusCode = response.statusCode();

                if (statusCode != 200) {
                    System.out.println("\nBot: [API Error] HTTP " + statusCode);
                    System.out.println(responseBody);
                    continue;
                }

                String extractedReply = extractGeminiText(responseBody);
                if (extractedReply.equals("Could not parse response.") || extractedReply.startsWith("[API Error]")) {
                    System.out.println("\nBot: " + extractedReply);
                    System.out.println(responseBody);
                    continue;
                }
                System.out.println("\nBot: " + extractedReply);
                
            } catch (Exception e) {
                System.out.println("Error communicating with Gemini API: " + e.getMessage());
            }
        }
    }

    // Robust JSON manual extractor to avoid external JSON dependencies
    private static String extractGeminiText(String json) {
        String marker = "\"text\": \"";
        int start = json.indexOf(marker);
        if (start == -1) {
            return "Could not parse response.";
        }
        start += marker.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == 'n') { sb.append('\n'); i++; }
                else if (next == '"') { sb.append('"'); i++; }
                else if (next == 't') { sb.append('\t'); i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else { sb.append(c); }
            } else if (c == '"') {
                break; // End of string value
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
    // ------------------------------

    // Input Verifiers
    public static boolean verifyTimeInput(String time) {
        try {
            LocalTime.parse(time, DateTimeFormatter.ofPattern("hh:mm a", Locale.US));
            return true;
        } catch (DateTimeParseException e) {
            System.out.println("The time entered is in incorrect format ,It should be like 06:00 PM");
            return false;
        }
    }

    public static boolean verifyDateInput(String date) {
        try {
            LocalDate.parse(date, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return true;
        } catch (DateTimeParseException e) {
            System.out.println("The date entered is in incoorect format ,It should be like 02/02/2024");
            return false;
        }
    }

    public static boolean verifyGenderInput(String gender) {
        if ((gender.trim().equalsIgnoreCase("male")) || (gender.trim().equalsIgnoreCase("female")) || (gender.trim().equalsIgnoreCase("other"))) {
            return true;
        }
        System.out.println("Invalid entry, male/female/other are allowed");
        return false;
    }

    public static void registerNewDoctor() {
        System.out.print(" You are Registering a new Doctor, Enter -1 anytime to discard the process \n");
        System.out.print("Enter doctor name: ");
        String name = scanner.nextLine();
        if (name.trim().equals("-1")) return;

        String timingStart;
        do {
            System.out.print("Enter doctor`s shift start time (e.g 10:00 AM): ");
            timingStart = scanner.nextLine();
            if (timingStart.trim().equals("-1")) return;
        } while (!verifyTimeInput(timingStart));

        String timingEnd;
        do {
            System.out.print("Enter doctor`s shift end time(e.g 03:00 PM): ");
            timingEnd = scanner.nextLine();
            if (timingEnd.trim().equals("-1")) return;
        } while (!verifyTimeInput(timingEnd));

        System.out.print("Enter doctor ward(e.g Cardiology): ");
        String ward = scanner.nextLine();
        if (ward.trim().equals("-1")) return;
        System.out.print("Enter doctor specialty(e.g Cardiologist): ");
        String specialty = scanner.nextLine();
        if (specialty.trim().equals("-1")) return;

        String username;
        do {
            System.out.print("Enter doctor username: ");
            username = scanner.nextLine();
            if (username.trim().equals("-1")) return;
        } while (entityExist(username, 2));

        System.out.print("Enter doctor password: ");
        String password = scanner.nextLine();
        if (password.trim().equals("-1")) return;

        String[] newDoctor = {generateID("Doctor"), name, timingStart, timingEnd, ward, specialty, username, password};
        doctorsList.add(newDoctor);

        syncListToDatabase("Doctors", doctorsList);
        System.out.println("New doctor added successfully.");
    }

    public static void handleEditDoctorDetailsMenu() {
        int index = getEntityIndexByIDNameSelection("Edit", "Doctor", doctorsList);
        if (index != -1) editDoctor(index);
    }

    public static void editDoctor(int index) {
        String[] doctorDetails = doctorsList.get(index);
        System.out.println("Current Details of Doctor '" + index + "': ");
        headingsDisplayer("Doctor", doctorDetails, true);
        headingsDisplayer("Doctor", doctorDetails, false);

        System.out.println("\nSelect the option to edit:\n1. Edit Doctor Name\n2. Edit Doctor Start Time\n3. Edit Doctor End Time\n4. Edit Doctor Ward\n5. Edit Doctor Specialty\n6. Cancel Operation");
        System.out.print("Enter your choice: ");
        int editChoice = scanner.nextInt();
        scanner.nextLine();

        switch (editChoice) {
            case 1: System.out.print("Enter new doctor name: "); doctorDetails[1] = scanner.nextLine(); break;
            case 2:
                String newStartTime;
                do {
                    System.out.print("Enter doctor`s shift start time (e.g 10:00 AM): ");
                    newStartTime = scanner.nextLine();
                } while (!verifyTimeInput(newStartTime));
                doctorDetails[2] = newStartTime; break;
            case 3:
                String newEndTime;
                do {
                    System.out.print("Enter doctor`s shift start time (e.g 10:00 AM): ");
                    newEndTime = scanner.nextLine();
                } while (!verifyTimeInput(newEndTime));
                doctorDetails[3] = newEndTime; break;
            case 4: System.out.print("Enter new ward: "); doctorDetails[4] = scanner.nextLine(); break;
            case 5: System.out.print("Enter new specialty: "); doctorDetails[5] = scanner.nextLine(); break;
            case 6: System.out.println("No changes made."); return;
            default: System.out.println("Invalid edit choice. No changes made.");
        }

        syncListToDatabase("Doctors", doctorsList);
        System.out.println("Details of Doctor '" + doctorDetails[0] + "' after edit: ");
        headingsDisplayer("Doctor", doctorDetails, true);
        headingsDisplayer("Doctor", doctorDetails, false);
    }

    public static int getEntityIndexByIDNameSelection(String operationToPerform, String entity, ArrayList<String[]> list) {
        int option = getNameIDSelection(operationToPerform, entity);
        int index = -1;
        switch (option) {
            case 0: return -1;
            case 1:
                index = getEntityIndexByNameInput(entity, list);
                if (index != -2) break;
            case 2:
                index = getEntityIndexByIDInput(entity, list);
                break;
            default: System.out.println("Invalid edit option.");
        }
        return index;
    }

    public static void handleGetDoctorDetails() {
        int index = getEntityIndexByIDNameSelection("Show", "Doctor", doctorsList);
        if (index != -1) showDoctorDetails(index);
    }

    public static void showDoctorDetails(int index) {
        String[] doctorDetails = doctorsList.get(index);
        System.out.println("Details of Doctor '" + doctorDetails[1] + "': ");
        headingsDisplayer("Doctor", doctorDetails, true);
        headingsDisplayer("Doctor", doctorDetails, false);
    }

    public static void handleRemoveDoctor() {
        int index = getEntityIndexByIDNameSelection("Remove", "Doctor", doctorsList);
        if (index != -1) removeDoctor(index);
    }

    public static void removeDoctor(int index) {
        doctorsList.remove(index);
        syncListToDatabase("Doctors", doctorsList);
        System.out.println("Doctor has been deleted.");
    }

    private static void handleDisplayAllDoctors() {
        System.out.println("\nList of Doctors:");
        System.out.printf("%-10s %-20s %-15s %-15s %-20s %-20s %-15s\n", "Doctor ID", "Doctor Name", "Start Time", "End Time", "Ward", "Specialty", "Appointments Count");
        for (String[] doctor : doctorsList) {
            long appointmentsCount = countAppointmentsForDoctor(doctor[0]);
            System.out.printf("%-10s %-20s %-15s %-15s %-20s %-20s %-15s\n", doctor[0], doctor[1], doctor[2], doctor[3], doctor[4], doctor[5], appointmentsCount);
        }
    }

    public static long countAppointmentsForDoctor(String doctorId) {
        int count = 0;
        for (String[] appointment : appointmentsList) {
            if (appointment[2].equals(doctorId)) ++count;
        }
        return count;
    }

    public static void handleRegisterNewReceptionist() {
        System.out.print(" You are Registering a new Receptionist, Enter -1 anytime to discard the process \n");
        System.out.print("Enter Receptionists name: ");
        String name = scanner.nextLine();
        if (name.trim().equals("-1")) return;

        String username;
        do {
            System.out.print("Enter Receptionist`s username: ");
            username = scanner.nextLine();
            if (username.trim().equals("-1")) return;
        } while (entityExist(username, 3));

        System.out.print("Enter Receptionist`s password: ");
        String password = scanner.nextLine();
        if (password.trim().equals("-1")) return;

        String[] newReceptionist = {generateID("Receptionist"), name, username, password};
        receptionistsList.add(newReceptionist);

        syncListToDatabase("Receptionists", receptionistsList);
        System.out.println("New Receptionist added successfully.");
    }

    public static void handleRemoveReceptionist() {
        int index = getEntityIndexByIDNameSelection("Remove", "Receptionist", receptionistsList);
        if (index != -1) removeReceptionist(index);
    }

    public static void removeReceptionist(int index) {
        receptionistsList.remove(index);
        syncListToDatabase("Receptionists", receptionistsList);
        System.out.println("Receptionist with ID '" + index + "' has been deleted.");
    }

    public static void handleEditReceptionistDetailsMenu() {
        int index = getEntityIndexByIDNameSelection("Edit", "Receptionist", receptionistsList);
        if (index != -1) editReceptionist(index);
    }

    public static void editReceptionist(int index) {
        String[] details = receptionistsList.get(index);
        System.out.println("Current Details of Receptionist '" + index + "': ");
        headingsDisplayer("Receptionist", details, true);
        headingsDisplayer("Receptionist", details, false);

        System.out.println("\nSelect the option to edit:\n1. Edit Receptionist Name\n2. Edit Receptionist UserName\n3. Edit Receptionist Password\n0. Cancel Operation");
        System.out.print("Enter your choice: ");
        while (!scanner.hasNextInt()) {
            scanner.nextLine();
            System.out.println("Please Enter only numbers");
            System.out.print("Enter your choice: ");
        }
        int editChoice = scanner.nextInt();
        scanner.nextLine();

        switch (editChoice) {
            case 1: System.out.print("Enter new Receptionist name: "); details[1] = scanner.nextLine(); break;
            case 2: System.out.print("Enter new UserName: "); details[2] = scanner.nextLine(); break;
            case 3: System.out.print("Enter new Password: "); details[3] = scanner.nextLine(); break;
            case 0: System.out.println("No changes made."); return;
            default: System.out.println("Invalid edit choice. No changes made.");
        }
        syncListToDatabase("Receptionists", receptionistsList);
        System.out.println("Details of Receptionist '" + details[0] + "' after edit: ");
        headingsDisplayer("Receptionist", details, true);
        headingsDisplayer("Receptionist", details, false);
    }

    public static void displayAllReceptionists() {
        System.out.println("\nList of Receptionists:");
        System.out.printf("%-20s %-20s %-15s %-15s\n", "Receptionist ID", "Receptionist Name", "User Name", "Password");
        for (String[] r : receptionistsList) {
            System.out.printf("%-20s %-20s %-15s %-15s\n", r[0], r[1], r[2], r[3]);
        }
    }

    public static void addWard() {
        System.out.print(" You are Registering a new Ward, \nEnter -1 anytime to discard the process \n");
        System.out.print("Enter Ward name: ");
        String name = scanner.nextLine();
        if (name.trim().equals("-1")) return;
        System.out.print("Enter Total number of Beds in the ward: ");
        String totalBeds = scanner.nextLine();
        if (totalBeds.trim().equals("-1")) return;
        System.out.print("Enter Number of Beds occupied (If any): ");
        String bedsOccupied = scanner.nextLine();
        if (bedsOccupied.trim().equals("-1")) return;
        System.out.print("Enter Ward Type: ");
        String wardType = scanner.nextLine();

        String[] newWard = {generateID("Ward"), name, totalBeds, bedsOccupied, wardType};
        wardsList.add(newWard);

        syncListToDatabase("Wards", wardsList);
        System.out.println("New Ward added successfully.");
    }

    public static void handleEditWardDetails() {
        int index = getEntityIndexByIDNameSelection("Edit", "Ward", wardsList);
        if (index != -1) editWard(index);
    }

    public static void editWard(int index) {
        String[] wardDetails = wardsList.get(index);
        System.out.println("Current Details of Ward '" + wardDetails[0] + "': ");
        headingsDisplayer("Ward", wardDetails, true);
        headingsDisplayer("Ward", wardDetails, false);

        System.out.println("\nSelect the option to edit:\n1. Edit Ward Name\n2. Edit Ward Type\n3. Edit Total Ward Beds\n0. Return: ");
        int editChoice = scanner.nextInt();
        scanner.nextLine();

        switch (editChoice) {
            case 1: System.out.print("Enter New Ward name: "); wardDetails[1] = scanner.nextLine(); break;
            case 2: System.out.print("Enter New Ward Type: "); wardDetails[4] = scanner.nextLine(); break;
            case 3:
                System.out.print("Enter New Total Number of Beds: ");
                int intNumberOfBedsOccupied = Integer.parseInt(wardDetails[3]);
                String newTotalNumberOfBeds = scanner.nextLine();
                int intNewTotalNumberOfBeds = Integer.parseInt(newTotalNumberOfBeds);
                if (intNewTotalNumberOfBeds >= intNumberOfBedsOccupied) {
                    wardDetails[2] = newTotalNumberOfBeds;
                } else {
                    System.out.println("Cannot Reduce Beds as there are Patients admitted in the Ward");
                    return;
                }
                break;
            case 0: System.out.println("No changes made."); return;
            default: System.out.println("Invalid edit choice. No changes made.");
        }
        syncListToDatabase("Wards", wardsList);
        System.out.println("Details of Ward '" + wardDetails[0] + "' after edit: ");
        headingsDisplayer("Ward", wardDetails, true);
        headingsDisplayer("Ward", wardDetails, false);
    }

    public static void displayAllWards() {
        System.out.println("\nList of Wards:");
        System.out.printf("%-10s %-20s %-15s %-15s\n", "Ward ID", "Ward Name", "Total Beds", "Occupied Beds");
        for (String[] ward : wardsList) {
            System.out.printf("%-10s %-20s %-15s %-15s\n", ward[0], ward[1], ward[2], ward[3]);
        }
    }

    public static void handleRemoveWard() {
        int index = getEntityIndexByIDNameSelection("Remove", "Ward", wardsList);
        if (index != -1) removeWard(index);
    }

    public static void removeWard(int index) {
        wardsList.remove(index);
        syncListToDatabase("Wards", wardsList);
        System.out.println("Ward has been deleted.");
    }

    public static void handleGetWardDetails() {
        int index = getEntityIndexByIDNameSelection("Show", "Ward", wardsList);
        if (index != -1) showWardDetails(index);
    }

    public static void showWardDetails(int index) {
        String[] wardDetails = wardsList.get(index);
        headingsDisplayer("Ward", wardDetails, true);
        headingsDisplayer("Ward", wardDetails, false);
        getWardPatients(wardDetails[0]);
    }

    public static boolean handlePatient() {
        int index = getEntityIndexByIDNameSelection("Select", "Patient", patientsList);
        if (index == -1) return false;
        selectedPatientIndex = index;
        return true;
    }

    public static void getPatientHistory() {
        getPatientDetails(selectedPatientIndex);
        String patientID = patientsList.get(selectedPatientIndex)[0];
        String patientName = patientsList.get(selectedPatientIndex)[1];

        System.out.println("All Appointments of Patient '" + patientName + "': ");
        boolean appointmentHeadingsShowed = false;
        for (String[] appointmentDetails : appointmentsList) {
            if (patientID.equals(appointmentDetails[1])) {
                if (!appointmentHeadingsShowed) {
                    headingsDisplayer("Appointment", appointmentDetails, true);
                    appointmentHeadingsShowed = true;
                }
                headingsDisplayer("Appointment", appointmentDetails, false);
            }
        }

        System.out.println("All Diagnosis of Patient '" + patientName + "': ");
        boolean patientHeadingsShowed = false;
        for (String[] diagnosisDetails : diagnosisList) {
            if (patientID.equals(diagnosisDetails[1])) {
                if (!patientHeadingsShowed) {
                    headingsDisplayer("Diagnosis", diagnosisDetails, true);
                    patientHeadingsShowed = true;
                }
                headingsDisplayer("Diagnosis", diagnosisDetails, false);
            }
        }
    }

    public static void addDiagnosis() {
        String patientID = patientsList.get(selectedPatientIndex)[0];
        String doctorID = doctorsList.get(loggedInDoctorIndex)[0];

        String diagnosisID = generateID("Diagnosis");
        System.out.print("Enter Prescriptions: ");
        String prescriptions = scanner.nextLine();
        System.out.print("Enter Diagnosis: ");
        String diagnosis = scanner.nextLine();

        String[] newDiagnosis = {diagnosisID, patientID, doctorID, prescriptions, diagnosis};
        diagnosisList.add(newDiagnosis);
        syncListToDatabase("Diagnosis", diagnosisList);
        System.out.println("Diagnosis information added successfully.");
    }

    public static void checkUpcomingAppointments() {
        boolean headingsShowed = false;
        for (String[] app : appointmentsList) {
            if (doctorsList.get(loggedInDoctorIndex)[0].equals(app[2]) && app[5].equals("Pending")) {
                if (!headingsShowed) {
                    headingsDisplayer("Appointment", app, true);
                    headingsShowed = true;
                }
                headingsDisplayer("Appointment", app, false);
            }
        }
    }

    public static void addPatient() {
        System.out.println("You are Registering a new Patient, Enter -1 anytime to discard the process");
        System.out.print("Enter patient name: ");
        String name = scanner.nextLine();
        if (name.trim().equals("-1")) return;

        String gender;
        do {
            System.out.print("Enter patient gender: ");
            gender = scanner.nextLine();
            if (gender.trim().equals("-1")) return;
        } while (!verifyGenderInput(gender));

        System.out.print("Enter patient age: ");
        String age = scanner.nextLine();
        if (age.trim().equals("-1")) return;

        System.out.print("Enter patient contact: ");
        String contact = scanner.nextLine();
        if (contact.trim().equals("-1")) return;

        String[] newPatient = {generateID("Patient"), name, gender, age, contact};
        patientsList.add(newPatient);
        syncListToDatabase("Patients", patientsList);
        System.out.println("New patient added successfully.");
    }

    private static void handleGetPatientDetails() {
        int index = getEntityIndexByIDNameSelection("Get Details", "Patient", patientsList);
        if (index != -1) getPatientDetails(index);
    }

    private static void getPatientDetails(int index) {
        String[] patientDetails = patientsList.get(index);
        System.out.println("Details of Patient '" + patientDetails[1] + "': ");
        headingsDisplayer("Patient", patientDetails, true);
        headingsDisplayer("Patient", patientDetails, false);
    }

    public static void handleEditPatientDetailsMenu() {
        int index = getEntityIndexByIDNameSelection("Edit", "Patient", patientsList);
        if (index != -1) editPatient(index);
    }

    public static void editPatient(int index) {
        String[] details = patientsList.get(index);
        System.out.println("Current Details of Patient '" + index + "': ");
        headingsDisplayer("Patient", details, true);
        headingsDisplayer("Patient", details, false);

        System.out.println("\nSelect the option to edit:\n1. Edit Patient Name\n2. Edit Patient Gender\n3. Edit Patient Age\n4. Edit Patient Contact\n0. Cancel Operation");
        System.out.print("Enter your choice: ");
        while (!scanner.hasNextInt()) {
            scanner.nextLine();
            System.out.println("Please Enter only numbers");
            System.out.print("Enter your choice: ");
        }
        int editChoice = scanner.nextInt();
        scanner.nextLine();

        switch (editChoice) {
            case 1: System.out.print("Enter new Patient name: "); details[1] = scanner.nextLine(); break;
            case 2: System.out.print("Enter new Gender: "); details[2] = scanner.nextLine(); break;
            case 3: System.out.print("Enter new Age: "); details[3] = scanner.nextLine(); break;
            case 4: System.out.print("Enter new Contact: "); details[4] = scanner.nextLine(); break;
            case 0: System.out.println("No changes made."); return;
            default: System.out.println("Invalid edit choice. No changes made.");
        }
        syncListToDatabase("Patients", patientsList);
        System.out.println("Details of Patient '" + details[0] + "' after edit: ");
        headingsDisplayer("Patient", details, true);
        headingsDisplayer("Patient", details, false);
    }

    public static void handleAdmitPatientToWard() {
        int patientIndex = getEntityIndexByIDNameSelection("Admit", "Patient", patientsList);
        if (patientIndex == -1) return;
        int wardIndex = getEntityIndexByIDNameSelection("Admit", "Ward", wardsList);
        if (wardIndex == -1) return;

        admitPatientToWard(patientIndex, wardIndex);
    }

    public static void admitPatientToWard(int patientIndex, int wardIndex) {
        String[] ward = wardsList.get(wardIndex);
        String[] patient = patientsList.get(patientIndex);
        
        int occupiedBeds = Integer.parseInt(ward[3]);
        int totalBeds = Integer.parseInt(ward[2]);
        LocalDate dateToday = LocalDate.now(ZoneId.of("Asia/Karachi"));

        if (occupiedBeds + 1 > totalBeds) {
            System.out.printf("The ward %s is out of beds , cannot add more patients in it....\n", ward[0]);
        } else {
            String[] newSubmission = {generateID("Submission"), patient[0], ward[0], dateToday.toString(), LocalDate.MAX.toString(), "", "Submitted"};
            ward[3] = Integer.toString(occupiedBeds + 1);

            wardPatientSubmissionList.add(newSubmission);
            syncListToDatabase("Wards", wardsList);
            syncListToDatabase("Submissions", wardPatientSubmissionList);
            System.out.println();
            getWardPatients(newSubmission[2]);
        }
    }

    public static void getWardPatients(String wardID) {
        boolean headingsDisplayed = false;
        for (String[] sub : wardPatientSubmissionList) {
            if (sub[2].equals(wardID)) {
                int index = getEntityIndexByID(sub[1], patientsList);
                if (index != -1) {
                    String[] patient = patientsList.get(index);
                    if (!headingsDisplayed) {
                        headingsDisplayer("Patient", patient, true);
                        headingsDisplayed = true;
                    }
                    headingsDisplayer("Patient", patient, false);
                }
            }
        }
    }

    public static void handleCreateAppointment() {
        int doctorIndex = getEntityIndexByIDNameSelection("Select", "Doctor", doctorsList);
        if (doctorIndex == -1) return;
        int patientIndex = getEntityIndexByIDNameSelection("Select", "Patient", patientsList);
        if (patientIndex == -1) return;
        
        String[] doctor = doctorsList.get(doctorIndex);
        String[] patient = patientsList.get(patientIndex);

        String appointmentTime;
        do {
            System.out.print("Enter appointment time (e.g 10:00 AM): ");
            appointmentTime = scanner.nextLine();
        } while (!verifyTimeInput(appointmentTime));
        appointmentTime = LocalTime.parse(appointmentTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.US)).toString();

        String appointmentDate;
        do {
            System.out.print("Enter appointment Date (e.g dd/MM/yyyy): ");
            appointmentDate = scanner.nextLine();
        } while (!verifyDateInput(appointmentDate));
        appointmentDate = LocalDate.parse(appointmentDate, DateTimeFormatter.ofPattern("dd/MM/yyyy")).toString();

        String[] newAppointment = {generateID("Appointment"), patient[0], doctor[0], appointmentTime, appointmentDate, "Pending"};
        appointmentsList.add(newAppointment);

        syncListToDatabase("Appointments", appointmentsList);
        System.out.println("The Appointment " + newAppointment[0] + " is registered with " + doctor[1] + " at " + appointmentTime + " " + appointmentDate);
    }

    public static void handleAppointmentStatus() {
        int index = getEntityIndexByIDInput("Appointment", appointmentsList);
        if (index != -1) {
            String[] appointment = appointmentsList.get(index);
            appointment[5] = "Completed";
            syncListToDatabase("Appointments", appointmentsList);
            System.out.println("The Appointment " + appointment[0] + " is marked as completed");
        }
    }

    public static void handlecheckDoctorsAvailability() {
        int index = getEntityIndexByIDNameSelection("Check Availability", "Doctor", doctorsList);
        if (index != -1) checkDoctorsAvailability(index);
    }

    public static void checkDoctorsAvailability(int index) {
        String[] doctor = doctorsList.get(index);
        LocalTime now = LocalTime.now();
        boolean isAvailable = isTimeInRange(now.toString(), doctor[2], doctor[3]);
        if (isAvailable)
            System.out.println("The Doctor is Available ");
        else
            System.out.println("The docotor " + doctor[1] + " is not avaiable,Shift Times are " + doctor[2] + "-" + doctor[3]);
    }

    public static void handleCheckoutPatient() {
        int index = getEntityIndexByIDNameSelection("Checkout", "Patient", patientsList);
        if (index != -1) checkoutPatient(index);
    }

    public static void checkoutPatient(int index) {
        String patientID = patientsList.get(index)[0];
        boolean found = false;
        
        for (String[] submission : wardPatientSubmissionList) {
            if (patientID.equals(submission[1]) && submission[6].equals("Submitted")) {
                System.out.println("Enter Reason for Checkout: ");
                submission[5] = scanner.nextLine();
                submission[6] = "Checked Out";
                found = true;
                
                String[] ward = wardsList.get(getEntityIndexByID(submission[2], wardsList));
                int occupiedBeds = Integer.parseInt(ward[3]);
                ward[3] = Integer.toString(occupiedBeds - 1);

                syncListToDatabase("Submissions", wardPatientSubmissionList);
                syncListToDatabase("Wards", wardsList);
            }
        }
        if (!found) System.out.println("The patient is not submitted or already checked out....");
    }

    public static void getAllDoctors() {
        System.out.println("\nList of Doctors:");
        System.out.printf("%-10s %-20s %-15s %-15s %-20s %-20s %-15s %-15s\n", "Doctor ID", "Doctor Name", "Start Time", "End Time", "Ward", "Specialty", "Username", "Password");
        for (String[] doctor : doctorsList) {
            System.out.printf("%-10s %-20s %-15s %-15s %-20s %-20s %-15s %-15s\n", doctor[0], doctor[1], doctor[2], doctor[3], doctor[4], doctor[5], doctor[6], doctor[7]);
        }
    }

    // Helper Functions
    public static boolean isTimeInRange(String time, String startTime, String endTime) {
        try {
            LocalTime timeToCheck = LocalTime.parse(time);
            LocalTime start = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.US));
            LocalTime end = LocalTime.parse(endTime, DateTimeFormatter.ofPattern("hh:mm a", Locale.US));

            if (start.isAfter(end)) return timeToCheck.isAfter(start) || timeToCheck.isBefore(end);
            else return timeToCheck.isAfter(start) && timeToCheck.isBefore(end);
        } catch (DateTimeParseException e) {
            System.out.println("Something went wrong!, Please make sure data is correct.");
            return false;
        }
    }

    public static int getNameIDSelection(String operation, String entity) {
        System.out.printf("\nSelect the option to %s %s: \n1. %s by Name\n2. %s by ID\n0. Return back \n", operation, entity, operation, operation);
        System.out.print("Enter your choice: ");
        while (!scanner.hasNextInt()) {
            scanner.nextLine();
            System.out.println("Only number are allowed!");
            System.out.print("Enter your choice: ");
        }
        int selection = scanner.nextInt();
        scanner.nextLine();
        return selection;
    }

    public static int getEntityIndexByNameInput(String entityName, ArrayList<String[]> list) {
        System.out.print("Enter the Name: ");
        String nameToView = scanner.nextLine();
        ArrayList<Integer> indices = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i)[1].contains(nameToView)) indices.add(i);
        }
        
        if (indices.size() > 1) {
            System.out.println("Multiple " + entityName + "s have found: ");
            for (Integer index : indices) System.out.println(Arrays.toString(list.get(index)));
            System.out.println("Enter the ID of the specified " + entityName);
            return -2;
        } else if (indices.isEmpty()) {
            System.out.println(entityName + " by the name " + nameToView + " not found.");
        } else {
            return indices.get(0);
        }
        return -1;
    }

    public static int getEntityIndexByIDInput(String entityName, ArrayList<String[]> list) {
        String entityID;
        do {
            System.out.print("Enter the ID: ");
            entityID = scanner.nextLine();
        } while (!verifyIDFormat(entityID, entityName));

        if (entityID.split("-").length > 1) entityID = entityID.split("-")[1];
        int idInteger = Integer.parseInt(entityID);

        int index = getEntityIndex(entityName, idInteger, list);
        if (index != -1) return index;
        else System.out.println(entityName + " by the ID " + entityID + " not found.");
        return -1;
    }

    public static boolean verifyIDFormat(String enteredID, String entityName) {
        if (enteredID.chars().allMatch(Character::isDigit)) return true;
        String[] parts = enteredID.split("-");
        if (parts.length > 2) return false;
        
        String entityPrefix = getEntityIDPrefix(entityName);
        entityPrefix = entityPrefix.substring(0, entityPrefix.length() - 1);
        boolean isFormatted = parts[0].equalsIgnoreCase(entityPrefix) && parts[1].chars().allMatch(Character::isDigit);
        if (!isFormatted) System.out.println("The ID formate is invalid, It should be a number or should have " + entityPrefix + "-097 format");
        return isFormatted;
    }

    public static int getEntityIndexByID(String entityID, ArrayList<String[]> list) {
        for (int i = 0; i < list.size(); ++i) {
            if (list.get(i)[0].equals(entityID)) return i;
        }
        return -1;
    }

    public static void headingsDisplayer(String entity, String[] array, boolean showHeadings) {
        switch (entity) {
            case "Doctor":
                if (showHeadings) System.out.printf("%-10s %-20s %-15s %-15s %-20s %-20s %-15s\n", "Doctor ID", "Doctor Name", "Start Time", "End Time", "Ward", "Specialty", "Appointments Count");
                else System.out.printf("%-10s %-20s %-15s %-15s %-20s %-20s %-15s\n", array[0], array[1], array[2], array[3], array[4], array[5], countAppointmentsForDoctor(array[0]));
                break;
            case "Appointment":
                if (showHeadings) System.out.printf("%-15s %-15s %-15s %-20s %-15s %-20s\n", "Appointment ID", "Patient ID", "Doctor ID", "Appointment Time", "Appointment Date", "Appointment Status");
                else System.out.printf("%-15s %-15s %-15s %-20s %-15s %-20s\n", array[0], array[1], array[2], array[3], array[4], array[5]);
                break;
            case "Diagnosis":
                if (showHeadings) System.out.printf("%-15s %-15s %-15s %-20s %-20s\n", "Diagnosis ID", "Patient ID", "Doctor ID", "Prescriptions", "Diagnosis");
                else System.out.printf("%-15s %-15s %-15s %-20s %-20s\n", array[0], array[1], array[2], array[3], array[4]);
                break;
            case "Patient":
                if (showHeadings) System.out.printf("%-15s %-20s %-10s %-5s %-15s\n", "Patient ID", "Patient Name", "Gender", "Age", "Contact");
                else System.out.printf("%-15s %-20s %-10s %-5s %-15s\n", array[0], array[1], array[2], array[3], array[4]);
                break;
            case "Receptionist":
                if (showHeadings) System.out.printf("%-15s %-20s %-15s %-15s\n", "Receptionist ID", "Receptionist Name", "Username", "Password");
                else System.out.printf("%-15s %-20s %-15s %-15s\n", array[0], array[1], array[2], array[3]);
                break;
            case "Ward":
                if (showHeadings) System.out.printf("%-15s %-20s %-15s %-15s %-10s\n", "Ward ID", "Ward Name", "Total Beds", "Occupied Beds", "Type");
                else System.out.printf("%-15s %-20s %-15s %-15s %-10s\n", array[0], array[1], array[2], array[3], array[4]);
                break;
        }
    }

    public static boolean verifyLoginDetails(int role) {
        int loginTries = 1;
        while (!getCredentials(role)) {
            System.out.println("Invalid Login Details! " + (5 - loginTries) + " tries left.");
            if (loginTries == 5) {
                System.out.println("You have entered wrong credentials " + loginTries + " times, returning to Main Menu");
                return false;
            }
            loginTries++;
        }
        System.out.println("Logged In Successfully");
        return true;
    }

    public static boolean getCredentials(int role) {
        System.out.print("Enter Your Username: ");
        String username = scanner.nextLine();
        System.out.print("Enter Your Password: ");
        String password = scanner.nextLine();

        if (role == 1) return username.equals(adminUsername) && password.equals(adminPassword);
        else if (role == 2) {
            for (String[] doc : doctorsList) {
                if (username.equals(doc[docUsernameIndex]) && password.equals(doc[docPasswordIndex])) {
                    loggedInDoctorIndex = getEntityIndexByID(doc[0], doctorsList);
                    return true;
                }
            }
        } else if (role == 3) {
            for (String[] rec : receptionistsList) {
                if (username.equals(rec[receptionistUsernameIndex]) && password.equals(rec[receptionistPasswordIndex])) return true;
            }
        }
        return false;
    }

    public static boolean isStrongPassword(String password) {
        if (password.length() < 8) return false;
        boolean hasDigit = false, hasUp = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) hasUp = true;
            if (Character.isDigit(ch)) hasDigit = true;
        }
        return hasDigit && hasUp;
    }

    public static boolean setAdminPassword(String username, String password) {
        if (isStrongPassword(password)) {
            adminUsername = username;
            adminPassword = password;
            System.out.println("\n Credentials set Successfully");
            return true;
        } else {
            System.out.println("\n Password is too weak, It must contain a Digit, UpperCaseLetter and should have length of 8");
            return false;
        }
    }

    public static void setAdminCredentialsForTheFirstTime() {
        System.out.println("The system is running for the first time!! . You are required to setup the admin Credentials");
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        String pass;
        do {
            System.out.print("Enter Password:");
            pass = scanner.nextLine();
        } while (!setAdminPassword(username, pass));
    }

    public static boolean entityExist(String username, int role) {
        if (role == 2) {
            for (String[] docInfo : doctorsList) {
                if (username.equals(docInfo[docUsernameIndex])) {
                    System.out.println("The username" + username + " already exist. Enter another.");
                    return true;
                }
            }
        } else if (role == 3) {
            for (String[] recepInfo : receptionistsList) {
                if (username.equals(recepInfo[receptionistUsernameIndex])) {
                    System.out.println("The username" + username + " already exist. Enter another.");
                    return true;
                }
            }
        }
        return false;
    }

    public static String getEntityIDPrefix(String entityName) {
        switch (entityName) {
            case "Doctor": return "DOC-";
            case "Receptionist": return "REP-";
            case "Ward": return "W-";
            case "Patient": return "P-";
            case "Diagnosis": return "DIA-";
            case "Appointment": return "APT-";
            case "Submission": return "SUB-";
            default: throw new IllegalArgumentException("Invalid Entity Type Passed");
        }
    }

    public static String getEntityID(String entityName, int id) {
        return getEntityIDPrefix(entityName) + String.format("%03d", id);
    }

    public static int getEntityIndex(String entityName, int id, ArrayList<String[]> list) {
        String did = getEntityID(entityName, id);
        for (int i = 0; i < list.size(); i++) {
            if (did.equals(list.get(i)[0])) return i;
        }
        return -1;
    }

    public static String generateID(String entityType) {
        int idx = switch (entityType) {
            case "Doctor" -> 0;
            case "Receptionist" -> 1;
            case "Ward" -> 2;
            case "Patient" -> 3;
            case "Diagnosis" -> 4;
            case "Appointment" -> 5;
            case "Submission" -> 6;
            default -> throw new IllegalArgumentException("Invalid Entity Type Passed");
        };
        numberOfEntitiesArray[idx]++;
        updateSystemVariablesFile(); // Save incremented ID logic
        return getEntityID(entityType, numberOfEntitiesArray[idx]);
    }

    // --- SQLite Database Operations ---

    public static void setupDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
             stmt.execute("CREATE TABLE IF NOT EXISTS SystemVariables (id INTEGER PRIMARY KEY, admin_user TEXT, admin_pass TEXT, c_doc INT, c_rec INT, c_ward INT, c_pat INT, c_diag INT, c_app INT, c_sub INT)");
             stmt.execute("CREATE TABLE IF NOT EXISTS Doctors (col1 TEXT, col2 TEXT, col3 TEXT, col4 TEXT, col5 TEXT, col6 TEXT, col7 TEXT, col8 TEXT)");
             stmt.execute("CREATE TABLE IF NOT EXISTS Receptionists (col1 TEXT, col2 TEXT, col3 TEXT, col4 TEXT)");
             stmt.execute("CREATE TABLE IF NOT EXISTS Wards (col1 TEXT, col2 TEXT, col3 TEXT, col4 TEXT, col5 TEXT)");
             stmt.execute("CREATE TABLE IF NOT EXISTS Patients (col1 TEXT, col2 TEXT, col3 TEXT, col4 TEXT, col5 TEXT)");
             stmt.execute("CREATE TABLE IF NOT EXISTS Diagnosis (col1 TEXT, col2 TEXT, col3 TEXT, col4 TEXT, col5 TEXT)");
             stmt.execute("CREATE TABLE IF NOT EXISTS Appointments (col1 TEXT, col2 TEXT, col3 TEXT, col4 TEXT, col5 TEXT, col6 TEXT)");
             stmt.execute("CREATE TABLE IF NOT EXISTS Submissions (col1 TEXT, col2 TEXT, col3 TEXT, col4 TEXT, col5 TEXT, col6 TEXT, col7 TEXT)");
        } catch (SQLException e) {
            System.err.println("Database setup failed: " + e.getMessage());
        }
    }

    public static boolean loadSystemVariablesFileIntoMemory() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM SystemVariables WHERE id=1")) {
             
             if (rs.next()) {
                 adminUsername = rs.getString("admin_user");
                 adminPassword = rs.getString("admin_pass");
                 for(int i = 0; i < 7; i++) {
                     numberOfEntitiesArray[i] = rs.getInt(i + 4);
                 }
                 return true;
             }
        } catch (SQLException e) {
            // Ignored, likely first run
        }
        return false;
    }

    public static void updateSystemVariablesFile() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
             stmt.execute("DELETE FROM SystemVariables");
             
             String sql = "INSERT INTO SystemVariables VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
             try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                 pstmt.setString(1, adminUsername);
                 pstmt.setString(2, adminPassword);
                 for (int i = 0; i < 7; i++) {
                     pstmt.setInt(i + 3, numberOfEntitiesArray[i]);
                 }
                 pstmt.executeUpdate();
             }
        } catch (SQLException e) {
            System.err.println("Error saving system variables: " + e.getMessage());
        }
    }

    public static void loadListFromDB(String tableName, ArrayList<String[]> list, int cols) {
        list.clear();
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName)) {
             
             while (rs.next()) {
                 String[] row = new String[cols];
                 for(int i = 0; i < cols; i++) row[i] = rs.getString(i + 1);
                 list.add(row);
             }
        } catch (SQLException e) {
            System.err.println("Error loading table " + tableName + ": " + e.getMessage());
        }
    }

    public static void loadDatabaseFilesIntoMemory() {
        loadListFromDB("Doctors", doctorsList, 8);
        loadListFromDB("Receptionists", receptionistsList, 4);
        loadListFromDB("Wards", wardsList, 5);
        loadListFromDB("Patients", patientsList, 5);
        loadListFromDB("Diagnosis", diagnosisList, 5);
        loadListFromDB("Appointments", appointmentsList, 6);
        loadListFromDB("Submissions", wardPatientSubmissionList, 7);
    }

    // Synchronizes the in-memory array list securely with SQLite
    public static void syncListToDatabase(String tableName, ArrayList<String[]> dataList) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("DELETE FROM " + tableName);
            if (dataList.isEmpty()) return;
            
            int cols = dataList.get(0).length;
            String placeholders = String.join(",", Collections.nCopies(cols, "?"));
            String sql = "INSERT INTO " + tableName + " VALUES (" + placeholders + ")";
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (String[] row : dataList) {
                    for (int i = 0; i < cols; i++) {
                        pstmt.setString(i + 1, row[i]);
                    }
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        } catch (SQLException e) {
            System.err.println("Error syncing table " + tableName + ": " + e.getMessage());
        }
    }

    // ----------------------------------------

    public static void InitializeProgramme() {
        setupDatabase();
        
        if (!loadSystemVariablesFileIntoMemory()) {
            setAdminCredentialsForTheFirstTime();
            updateSystemVariablesFile();
        } else {
            loadDatabaseFilesIntoMemory();
        }
    }

    public static void main(String[] args) {
        InitializeProgramme();
        navigateMenu(mainMenuOptions);
    }
}