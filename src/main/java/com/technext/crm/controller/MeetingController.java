package com.technext.crm.controller;

import com.technext.crm.model.Meeting;
import com.technext.crm.service.MeetingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/meetings")
@CrossOrigin(origins = "http://localhost:3000")
public class MeetingController {

    @Autowired
    private MeetingService meetingService;

    @GetMapping
    public List<Meeting> getAllMeetings() {
        return meetingService.getAllMeetings();
    }

    @GetMapping("/{id}")
    public Optional<Meeting> getMeetingById(@PathVariable Integer id) {
        return meetingService.getMeetingById(id);
    }

    @GetMapping("/owner/{ownerId}")
    public List<Meeting> getMeetingsByOwner(@PathVariable Integer ownerId) {
        return meetingService.getMeetingsByOwner(ownerId);
    }

    @GetMapping("/status/{status}")
    public List<Meeting> getMeetingsByStatus(@PathVariable String status) {
        return meetingService.getMeetingsByStatus(status);
    }

    @PostMapping
    public Meeting createMeeting(@RequestBody Meeting meeting) {
        return meetingService.createMeeting(meeting);
    }

    @PutMapping("/{id}")
    public Meeting updateMeeting(@PathVariable Integer id, @RequestBody Meeting meeting) {
        return meetingService.updateMeeting(id, meeting);
    }

    @DeleteMapping("/{id}")
    public void deleteMeeting(@PathVariable Integer id) {
        meetingService.deleteMeeting(id);
    }
}