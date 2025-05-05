package com.fsf.habitup.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.fsf.habitup.Enums.HabitCategory;
import com.fsf.habitup.Repository.HabitRepo;
import com.fsf.habitup.Repository.UserRepository;
import com.fsf.habitup.entity.Habit;
import com.fsf.habitup.entity.User;

import jakarta.transaction.Transactional;

@Component
public class HabitInitToUsers implements CommandLineRunner {
    private final UserRepository userRepository;
    private final HabitRepo habitRepository;

    public HabitInitToUsers(UserRepository userRepository, HabitRepo habitRepository) {
        this.userRepository = userRepository;
        this.habitRepository = habitRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Create default habits if none exist
        List<Habit> existingHabits = habitRepository.findAll();
        List<Habit> habitsToAssign = existingHabits.isEmpty() ? createDefaultHabits() : new ArrayList<>(existingHabits);

        // 2. Assign all habits to all users
        List<User> users = userRepository.findAll();
        for (User user : users) {
            for (Habit habit : habitsToAssign) {
                if (!user.getHabits().contains(habit)) {
                    user.addHabit(habit);
                }
            }
        }

        // 3. Save all users
        userRepository.saveAll(users);
    }

    private List<Habit> createDefaultHabits() {
        return habitRepository.saveAll(
                Arrays.stream(HabitCategory.values())
                        .map(category -> {
                            Habit habit = new Habit();
                            habit.setHabitName(category.getDisplayName());
                            habit.setHabitCategory(category);
                            habit.setHabitDescription(getDefaultDescription(category)); // Add this
                            habit.setHabitDuration(getDefaultDuration(category)); // Add this
                            return habit;
                        })
                        .collect(Collectors.toList()));
    }

    private String getDefaultDescription(HabitCategory category) {
        return switch (category) {
            case MEDITATION -> "Daily meditation practice";
            case READING -> "Reading for personal growth";
            case EXERCISE_AND_YOGA -> "Physical exercise and yoga";
            // have to add other category
            default -> category.getDisplayName() + " activity";
        };
    }

    private int getDefaultDuration(HabitCategory category) {
        return switch (category) {
            case MEDITATION -> 15;
            case READING -> 30;
            case EXERCISE_AND_YOGA -> 45;
            // Add durations for all other categories
            default -> 20;
        };
    }

}
