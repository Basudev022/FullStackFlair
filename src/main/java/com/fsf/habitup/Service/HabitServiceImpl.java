package com.fsf.habitup.Service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.fsf.habitup.Repository.HabitRepo;
import com.fsf.habitup.Repository.UserRepository;
import com.fsf.habitup.entity.Habit;
import com.fsf.habitup.entity.User;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

@Service
public class HabitServiceImpl implements HabitService {

    private final UserRepository userRepository;
    private final HabitRepo habitRepository;

    public HabitServiceImpl(UserRepository userRepository, HabitRepo habitRepository) {
        this.userRepository = userRepository;
        this.habitRepository = habitRepository;
    }

    @Override
    @Transactional
    public Habit addHabitToUser(Long userId, Long habitId, Integer customDuration) {
        // 1. Find user and validate
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        // 2. Find system habit and validate
        Habit systemHabit = habitRepository.findById(habitId)
                .orElseThrow(() -> new EntityNotFoundException("System habit not found with id: " + habitId));

        // 3. Check if user already has this habit
        boolean alreadyExists = user.getHabits().stream()
                .anyMatch(habit -> habit.getHabitId().equals(habitId));

        if (alreadyExists) {
            throw new IllegalStateException("User already has this habit");
        }

        // 4. Create user's habit (can customize if needed)
        Habit userHabit = new Habit();
        userHabit.setHabitName(systemHabit.getHabitName());
        userHabit.setHabitCategory(systemHabit.getHabitCategory());
        userHabit.setHabitDescription(systemHabit.getHabitDescription());
        userHabit.setHabitDuration(customDuration != null ? customDuration : systemHabit.getHabitDuration());
        userHabit.setHabitId(habitId); // Track origin if needed

        // 5. Associate with user
        user.getHabits().add(userHabit);
        userHabit.setUsers((Set<User>) user); // If bidirectional

        // 6. Save and return
        return habitRepository.save(userHabit);

    }

    @Override
    public void deleteHabit(Long habitId) {
        habitRepository.deleteById(habitId);
    }

    @Override
    public Habit updateHabit(Long habitId, Habit updatedHabit) {
        Habit existingHabit = habitRepository.findById(habitId)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found with id: " + habitId));

        existingHabit.setHabitName(updatedHabit.getHabitName());
        existingHabit.setHabitDescription(updatedHabit.getHabitDescription());
        existingHabit.setHabitDuration(updatedHabit.getHabitDuration());

        // 4. For enum fields, validate before updating
        if (updatedHabit.getHabitCategory() != null) {
            existingHabit.setHabitCategory(updatedHabit.getHabitCategory());
        }

        // 5. Save and return updated habit
        return habitRepository.save(existingHabit);
    }

    @Override
    public List<Habit> showAllHabits() {
        return habitRepository.findAll();
    }

    @Override
    public Habit showHabitById(Long habitId) {
        return habitRepository.findById(habitId)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found with id: " + habitId));
    }
}
