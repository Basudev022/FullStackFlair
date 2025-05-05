package com.fsf.habitup.Service;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.fsf.habitup.Enums.HabitCompletionStatus;
import com.fsf.habitup.Repository.HabitProgressRepository;
import com.fsf.habitup.Repository.HabitRepo;
import com.fsf.habitup.Repository.UserRepository;
import com.fsf.habitup.entity.Habit;
import com.fsf.habitup.entity.HabitProgress;
import com.fsf.habitup.entity.User;

import jakarta.transaction.Transactional;

@Service
public class HabitProgressImpl implements HabitProgressService {
        private final HabitRepo habitRepository;
        private final HabitProgressRepository habitProgressRepository;
        private final UserRepository userRepository;

        public HabitProgressImpl(HabitRepo habitRepository, HabitProgressRepository habitProgressRepository,
                        UserRepository userRepository) {
                this.habitRepository = habitRepository;
                this.habitProgressRepository = habitProgressRepository;
                this.userRepository = userRepository;
        }

        @Override
        @Transactional
        public void recordDailyCompletion(Long habitId, Long userId, LocalDate date, boolean completed) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Optional<HabitProgress> existingProgress = habitProgressRepository
                                .findByHabitAndUserAndProgressStartingDate(habit, user, date);

                HabitProgress habitProgress;
                if (existingProgress.isPresent()) {
                        habitProgress = existingProgress.get();
                } else {
                        habitProgress = new HabitProgress();
                        habitProgress.setHabit(habit);
                        habitProgress.setUser(user);
                        habitProgress.setProgressStartingDate(date);
                }
                habitProgress.setHabitCompletionStatus(completed
                                ? HabitCompletionStatus.COMPLETED
                                : HabitCompletionStatus.PENDING);

                if (completed) {
                        habitProgress.setStreakCount(habitProgress.getStreakCount() + 1);
                        habitProgress.setHabitScore(habitProgress.getHabitScore() + 10); // Example scoring system
                        habit.setTotalCompletions(habit.getTotalCompletions() + 1);
                        habit.setCurrentStreak(habit.getCurrentStreak() + 1);
                } else {
                        habit.setCurrentStreak(0);
                }

                habitProgressRepository.save(habitProgress);
                habitRepository.save(habit);
        }

        @Override
        @Transactional
        public void markHabitDone(Long habitId, Long userId) {
                recordDailyCompletion(habitId, userId, LocalDate.now(), true);

        }

        @Override
        public void toggleCompletionStatus(Long habitId, Long userId, LocalDate date) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                Optional<HabitProgress> existingProgress = habitProgressRepository
                                .findByHabitAndUserAndProgressStartingDate(habit, user, date);

                if (existingProgress.isPresent()) {
                        HabitProgress habitProgress = existingProgress.get();
                        boolean currentlyCompleted = habitProgress
                                        .getHabitCompletionStatus() == HabitCompletionStatus.COMPLETED;

                        habitProgress.setHabitCompletionStatus(currentlyCompleted
                                        ? HabitCompletionStatus.PENDING
                                        : HabitCompletionStatus.COMPLETED);

                        if (currentlyCompleted) {
                                habitProgress.setStreakCount(0);
                                habit.setCurrentStreak(0);
                        } else {
                                habitProgress.setStreakCount(habitProgress.getStreakCount() + 1);
                                habitProgress.setHabitScore(habitProgress.getHabitScore() + 10);
                                habit.setTotalCompletions(habit.getTotalCompletions() + 1);
                                habit.setCurrentStreak(habit.getCurrentStreak() + 1);
                        }

                        habitProgressRepository.save(habitProgress);
                        habitRepository.save(habit);
                } else {
                        recordDailyCompletion(habitId, userId, date, true);
                }
        }

        @Override
        public boolean getCompletionStatus(Long habitId, Long userId, LocalDate date) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return habitProgressRepository.findByHabitAndUserAndProgressStartingDate(habit, user, date)
                                .map(progress -> progress.getHabitCompletionStatus() == HabitCompletionStatus.COMPLETED)
                                .orElse(false);
        }

        @Override
        public boolean getTodayStatus(Long habitId, Long userId) {

                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Fetch habit progress for today
                Optional<HabitProgress> progress = habitProgressRepository
                                .findByHabitAndUserAndProgressStartingDate(habit, user, LocalDate.now());

                // Return true if completed, false otherwise
                return progress.isPresent()
                                && progress.get().getHabitCompletionStatus() == HabitCompletionStatus.COMPLETED;
        }

        @Override
        public Map<LocalDate, Boolean> getWeeklyStatus(Long habitId, Long userId) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get the current date and calculate the last 7 days
                LocalDate today = LocalDate.now();
                LocalDate startOfWeek = today.minusDays(7);

                // Initialize the map to hold the status for each day
                Map<LocalDate, Boolean> weeklyStatus = new HashMap<>();

                // Check habit progress for each day in the past week
                for (LocalDate date = startOfWeek; !date.isAfter(today); date = date.plusDays(1)) {
                        Optional<HabitProgress> progress = habitProgressRepository
                                        .findByHabitAndUserAndProgressStartingDate(habit, user, date);

                        // Map the date to completion status
                        weeklyStatus.put(date, progress.isPresent() && progress.get()
                                        .getHabitCompletionStatus() == HabitCompletionStatus.COMPLETED);
                }

                return weeklyStatus;
        }

        @Override
        public Map<YearMonth, Boolean> getMonthlyStatus(Long habitId, Long userId) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get the current date and calculate the last 6 months (or any custom period)
                LocalDate today = LocalDate.now();
                LocalDate startOfMonth = today.minusMonths(6).withDayOfMonth(1); // Adjust period here as needed

                // Initialize the map to hold the status for each month
                Map<YearMonth, Boolean> monthlyStatus = new HashMap<>();

                // Iterate through each month starting from the last 6 months
                for (LocalDate date = startOfMonth; !date.isAfter(today); date = date.plusMonths(1)) {
                        YearMonth yearMonth = YearMonth.from(date);

                        // Check if there was any habit progress for that month
                        boolean completedInMonth = habitProgressRepository
                                        .findByHabitAndUserAndProgressStartingDateBetween(habit, user,
                                                        date.withDayOfMonth(1),
                                                        date.withDayOfMonth(date.lengthOfMonth()))
                                        .stream()
                                        .anyMatch(progress -> progress
                                                        .getHabitCompletionStatus() == HabitCompletionStatus.COMPLETED);

                        // Map the YearMonth to completion status
                        monthlyStatus.put(yearMonth, completedInMonth);
                }

                return monthlyStatus;
        }

        @Override
        public int getCurrentStreak(Long habitId, Long userId) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get the habit progress records for this habit and user, sorted by date
                // descending
                List<HabitProgress> progressRecords = habitProgressRepository
                                .findByHabitAndUserOrderByProgressStartingDateDesc(habit, user);

                // Initialize streak counter
                int streak = 0;

                // Check for consecutive days where the habit was completed
                for (int i = 0; i < progressRecords.size(); i++) {
                        HabitProgress progress = progressRecords.get(i);

                        // If the habit was not completed on a particular day, break the streak
                        if (progress.getHabitCompletionStatus() != HabitCompletionStatus.COMPLETED) {
                                break;
                        }

                        // Check if the progress is on consecutive days
                        if (i == 0 || progressRecords.get(i - 1).getProgressStartingDate()
                                        .isEqual(progress.getProgressStartingDate().plusDays(1))) {
                                streak++;
                        } else {
                                break; // If not consecutive, stop the streak
                        }
                }

                return streak;
        }

        @Override
        public int getLongestStreak(Long habitId, Long userId) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get the habit progress records for this habit and user, sorted by date
                // ascending
                List<HabitProgress> progressRecords = habitProgressRepository
                                .findByHabitAndUserOrderByProgressStartingDateAsc(habit, user);

                // Variables to track the longest streak and current streak
                int longestStreak = 0;
                int currentStreak = 0;

                // Iterate through the records and check for consecutive completions
                for (int i = 0; i < progressRecords.size(); i++) {
                        HabitProgress progress = progressRecords.get(i);

                        // If habit is not completed, break the current streak
                        if (progress.getHabitCompletionStatus() != HabitCompletionStatus.COMPLETED) {
                                currentStreak = 0;
                        } else {
                                // For the first record, or if the progress is on consecutive days
                                if (i == 0 || progressRecords.get(i - 1).getProgressStartingDate()
                                                .isEqual(progress.getProgressStartingDate().minusDays(1))) {
                                        currentStreak++;
                                } else {
                                        // If not consecutive, reset the current streak
                                        currentStreak = 1;
                                }
                        }

                        // Update the longest streak
                        if (currentStreak > longestStreak) {
                                longestStreak = currentStreak;
                        }
                }

                return longestStreak;
        }

        @Override
        public double calculateConsistencyScore(Long habitId, Long userId) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get the habit progress records for this habit and user, sorted by date
                // ascending
                List<HabitProgress> progressRecords = habitProgressRepository
                                .findByHabitAndUserOrderByProgressStartingDateAsc(habit, user);

                if (progressRecords.isEmpty()) {
                        return 0.0; // No progress records, so no consistency
                }

                int totalDays = progressRecords.size();
                int completedDays = (int) progressRecords.stream()
                                .filter(progress -> progress
                                                .getHabitCompletionStatus() == HabitCompletionStatus.COMPLETED)
                                .count();

                // Directly return the completion rate (percentage)
                return (double) completedDays / totalDays * 100;
        }

        @Override
        public Map<LocalDate, Boolean> getMonthlyCompletionCalendar(Long habitId, Long userId) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get the first and last day of the current month
                YearMonth currentMonth = YearMonth.now();
                LocalDate startOfMonth = currentMonth.atDay(1);
                LocalDate endOfMonth = currentMonth.atEndOfMonth();

                // Fetch progress records for the habit and user within the month
                List<HabitProgress> progressRecords = habitProgressRepository
                                .findByHabitAndUserAndProgressStartingDateBetween(habit, user, startOfMonth,
                                                endOfMonth);

                // Create a map to store completion status for each day of the month
                Map<LocalDate, Boolean> completionCalendar = new HashMap<>();

                // Convert the list of progress records to a set of completed dates for faster
                // lookup
                Set<LocalDate> completedDates = progressRecords.stream()
                                .filter(progress -> progress
                                                .getHabitCompletionStatus() == HabitCompletionStatus.COMPLETED)
                                .map(HabitProgress::getProgressStartingDate)
                                .collect(Collectors.toSet());

                // Loop through each day of the month and mark completion status
                for (LocalDate date = startOfMonth; !date.isAfter(endOfMonth); date = date.plusDays(1)) {
                        // Check if the current date is in the set of completed dates
                        boolean completed = completedDates.contains(date);
                        completionCalendar.put(date, completed);
                }

                return completionCalendar;
        }

        @Override
        public Map<String, Object> getProgressStatistics(Long habitId, Long userId) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get the habit progress records for this habit and user
                List<HabitProgress> progressRecords = habitProgressRepository
                                .findByHabitAndUserOrderByProgressStartingDateAsc(habit, user);

                // Calculate Total Completions
                long totalCompletions = progressRecords.stream()
                                .filter(progress -> progress
                                                .getHabitCompletionStatus() == HabitCompletionStatus.COMPLETED)
                                .count();

                // Calculate Current Streak
                int currentStreak = habit.getCurrentStreak(); // This can be derived directly from the habit entity

                // Calculate Longest Streak
                int longestStreak = 0;
                int currentStreakCount = 0;
                for (HabitProgress progress : progressRecords) {
                        if (progress.getHabitCompletionStatus() == HabitCompletionStatus.COMPLETED) {
                                currentStreakCount++;
                                longestStreak = Math.max(longestStreak, currentStreakCount);
                        } else {
                                currentStreakCount = 0;
                        }
                }

                // Calculate Consistency Score
                double consistencyScore = calculateConsistencyScore(habitId, userId); // Reusing the existing method

                // Calculate Total Habit Score
                int totalHabitScore = progressRecords.stream()
                                .filter(progress -> progress
                                                .getHabitCompletionStatus() == HabitCompletionStatus.COMPLETED)
                                .mapToInt(HabitProgress::getHabitScore)
                                .sum();

                // Package the statistics into a Map
                Map<String, Object> statistics = new HashMap<>();
                statistics.put("totalCompletions", totalCompletions);
                statistics.put("currentStreak", currentStreak);
                statistics.put("longestStreak", longestStreak);
                statistics.put("consistencyScore", consistencyScore);
                statistics.put("totalHabitScore", totalHabitScore);

                return statistics;
        }

        @Override
        public double getSuccessRate(Long habitId, Long userId) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                // Fetch the User object
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get the habit progress records for this habit and user (you can adjust the
                // date range as needed)
                List<HabitProgress> progressRecords = habitProgressRepository
                                .findByHabitAndUser(habit, user); // Assuming you're calculating for all records

                // Calculate the total number of days and the number of completed days
                long totalDays = progressRecords.size();
                long completedDays = progressRecords.stream()
                                .filter(progress -> progress
                                                .getHabitCompletionStatus() == HabitCompletionStatus.COMPLETED)
                                .count();

                // Calculate and return the success rate
                if (totalDays == 0) {
                        return 0.0; // Avoid division by zero if no records are available
                }

                return (double) completedDays / totalDays * 100;
        }

        @Override
        public double predictHabitAdoption(Long habitId, Long userId) {
                int longestStreak = getLongestStreak(habitId, userId);

                // Predict habit adoption based on the longest streak
                // You can adjust the logic to your specific use case
                double adoptionProbability;

                if (longestStreak >= 10) {
                        adoptionProbability = 0.9; // High probability of adoption for long streaks
                } else if (longestStreak >= 5) {
                        adoptionProbability = 0.6; // Moderate probability for medium streaks
                } else {
                        adoptionProbability = 0.3; // Lower probability for shorter streaks
                }

                return adoptionProbability;
        }

        @Override
        public LocalDate predictHabitMasteryDate(Long habitId, Long userId) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get the habit progress records for this habit and user
                List<HabitProgress> progressRecords = habitProgressRepository
                                .findByHabitAndUserOrderByProgressStartingDateAsc(habit, user);

                // Calculate the total completions and current streak
                long totalCompletions = habit.getTotalCompletions();
                // int currentStreak = habit.getCurrentStreak();

                // Assume the user wants to achieve a certain number of completions for mastery
                int masteryGoal = 100; // Example goal for mastery, could be dynamic

                // If the user has already completed the mastery goal, return the current date
                // as mastery date
                if (totalCompletions >= masteryGoal) {
                        return LocalDate.now();
                }

                // Estimate the average completions per month based on the past progress
                long completionsInLastMonth = progressRecords.stream()
                                .filter(progress -> progress.getProgressStartingDate()
                                                .isAfter(LocalDate.now().minusMonths(1)))
                                .count();

                // Estimate how many months are needed to reach the mastery goal
                double estimatedMonths = (masteryGoal - totalCompletions) / (double) completionsInLastMonth;

                // Predict the mastery date
                LocalDate masteryDate = LocalDate.now().plusMonths((long) Math.ceil(estimatedMonths));

                return masteryDate;
        }

        @Override
        public String exportProgressData(Long habitId, Long userId) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get all the progress records for the given habit and user
                List<HabitProgress> progressRecords = habitProgressRepository
                                .findByHabitAndUser(habit, user);

                // Initialize a StringWriter to store the CSV data
                StringWriter stringWriter = new StringWriter();

                try {
                        // Write the CSV header
                        stringWriter.write("Date,Completion Status,Streak Count,Habit Score,Notes\n");

                        // Iterate through the progress records and write them to the CSV file
                        for (HabitProgress progress : progressRecords) {
                                String completionStatus = progress.getHabitCompletionStatus().toString();
                                stringWriter.write(String.format("%s,%s,%d,%d,%s\n",
                                                progress.getProgressStartingDate(),
                                                completionStatus,
                                                progress.getStreakCount(),
                                                progress.getHabitScore(),
                                                progress.getNotes() != null ? progress.getNotes() : ""));
                        }
                } catch (Exception e) { // Catch generic Exception here
                        throw new RuntimeException("Error exporting progress data", e);
                }

                // Return the CSV content as a string
                return stringWriter.toString();
        }

        @Override
        public void importProgressData(Long habitId, Long userId, String data) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Split the input data by line (CSV format)
                String[] lines = data.split("\n");

                // Skip the first line as it contains headers
                for (int i = 1; i < lines.length; i++) {
                        String line = lines[i];
                        String[] fields = line.split(",");

                        if (fields.length != 5) {
                                continue; // Skip invalid lines (if any)
                        }

                        try {
                                // Parse the individual fields from the CSV
                                LocalDate progressDate = LocalDate.parse(fields[0].trim()); // "Date"
                                HabitCompletionStatus completionStatus = HabitCompletionStatus
                                                .valueOf(fields[1].trim()); // "Completion Status"
                                int streakCount = Integer.parseInt(fields[2].trim()); // "Streak Count"
                                int habitScore = Integer.parseInt(fields[3].trim()); // "Habit Score"
                                String notes = fields[4].trim(); // "Notes"

                                // Check if a progress record for this date already exists for the given habit
                                // and user
                                Optional<HabitProgress> existingProgress = habitProgressRepository
                                                .findByHabitAndUserAndProgressStartingDate(habit, user, progressDate);

                                HabitProgress habitProgress;
                                if (existingProgress.isPresent()) {
                                        // Update the existing progress record
                                        habitProgress = existingProgress.get();
                                } else {
                                        // Create a new progress record
                                        habitProgress = new HabitProgress();
                                        habitProgress.setHabit(habit);
                                        habitProgress.setUser(user);
                                        habitProgress.setProgressStartingDate(progressDate);
                                }

                                // Set the fields in the HabitProgress entity
                                habitProgress.setHabitCompletionStatus(completionStatus);
                                habitProgress.setStreakCount(streakCount);
                                habitProgress.setHabitScore(habitScore);
                                habitProgress.setNotes(notes);

                                // Save or update the progress record
                                habitProgressRepository.save(habitProgress);

                        } catch (NumberFormatException e) {
                                // Log the error for the problematic line (if needed)
                                System.out.println("Error processing line: " + lines[i] + " - " + e.getMessage());
                                // Continue with the next line
                        }
                }
        }

        @Override
        public Map<LocalDate, String> getDailyNotes(Long habitId, Long userId) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Get the habit progress records for the given habit and user
                List<HabitProgress> progressRecords = habitProgressRepository
                                .findByHabitAndUser(habit, user);

                // Create a map to store the daily notes
                Map<LocalDate, String> dailyNotes = new HashMap<>();

                // Iterate through the progress records and populate the map
                for (HabitProgress progress : progressRecords) {
                        LocalDate date = progress.getProgressStartingDate();
                        String notes = progress.getNotes() != null ? progress.getNotes() : "";

                        // Add the date and corresponding note to the map
                        dailyNotes.put(date, notes);
                }

                return dailyNotes;
        }

        @Override
        public void addProgressNote(Long habitId, Long userId, LocalDate date, String note) {
                Habit habit = habitRepository.findById(habitId)
                                .orElseThrow(() -> new RuntimeException("Habit not found"));

                // Retrieve the user by userId
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                // Find the progress record for the given habit, user, and date
                HabitProgress habitProgress = habitProgressRepository
                                .findByHabitAndUserAndProgressStartingDate(habit, user, date)
                                .orElseThrow(() -> new RuntimeException(
                                                "No progress record found for the specified date"));

                // Update the progress record with the new note
                habitProgress.setNotes(note);

                // Save the updated habit progress
                habitProgressRepository.save(habitProgress);
        }

}
