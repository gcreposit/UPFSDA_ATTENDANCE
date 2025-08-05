// Mock data for the attendance system - Client side
const mockData = {
  employees: [
    {
      id: 1,
      username: "lokesh.kumar",
      name: "Lokesh Kumar",
      role: "UI/UX Designer",
      department: "Design",
      email: "lokesh@UPFSDA.com",
      phone: "+91 98765 43210",
      profileImage: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face&auto=format",
      workMode: "Office",
      isOnline: true,
      attendanceToday: {
        punchIn: "10:05",
        punchOut: null,
        workingHours: "06:43:37",
        status: "Working"
      },
      leaveBalance: {
        remaining: 16,
        used: 4,
        total: 20,
        annual: 12,
        sick: 6,
        personal: 2
      },
      projects: [
        {
          id: 1,
          name: "Mobile App Redesign",
          projectId: "PRJ-001",
          status: "In Progress",
          progress: 75,
          todayHours: "06:43",
          totalHours: "124:30",
          deadline: "2024-12-15",
          assignedTo: "Lokesh Kumar",
          assignedToId: "1",
          assignedToRole: "UI/UX Designer",
          assignedToImage: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face&auto=format"
        },
        {
          id: 2,
          name: "Website UI Revamp",
          projectId: "PRJ-002",
          status: "Planning",
          progress: 25,
          todayHours: "00:00",
          totalHours: "32:15",
          deadline: "2024-12-30",
          assignedTo: "Lokesh Kumar",
          assignedToId: "1",
          assignedToRole: "UI/UX Designer",
          assignedToImage: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150&h=150&fit=crop&crop=face&auto=format"
        }
      ]
    },
    {
      id: 2,
      username: "priya.sharma",
      name: "Priya Sharma",
      role: "Frontend Developer",
      department: "Development",
      email: "priya@UPFSDA.com",
      phone: "+91 87654 32109",
      profileImage: "https://images.unsplash.com/photo-1494790108755-2616b612b732?w=150&h=150&fit=crop&crop=face&auto=format",
      workMode: "Home",
      isOnline: true,
      attendanceToday: {
        punchIn: "09:30",
        punchOut: null,
        workingHours: "07:15:20",
        status: "Working"
      },
      leaveBalance: {
        remaining: 18,
        used: 2,
        total: 20,
        annual: 15,
        sick: 3,
        personal: 2
      },
      projects: [
        {
          id: 3,
          name: "Dashboard Development",
          projectId: "PRJ-003",
          status: "In Progress",
          progress: 90,
          todayHours: "07:15",
          totalHours: "89:45",
          deadline: "2024-11-30",
          assignedTo: "Priya Sharma",
          assignedToId: "2",
          assignedToRole: "Frontend Developer",
          assignedToImage: "https://images.unsplash.com/photo-1494790108755-2616b612b732?w=150&h=150&fit=crop&crop=face&auto=format"
        }
      ]
    },
    {
      id: 3,
      username: "rahul.gupta",
      name: "Rahul Gupta",
      role: "Backend Developer",
      department: "Development",
      email: "rahul@UPFSDA.com",
      phone: "+91 76543 21098",
      profileImage: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face&auto=format",
      workMode: "Office",
      isOnline: false,
      attendanceToday: {
        punchIn: "09:00",
        punchOut: "18:00",
        workingHours: "09:00:00",
        status: "Completed"
      },
      leaveBalance: {
        remaining: 14,
        used: 6,
        total: 20,
        annual: 10,
        sick: 4,
        personal: 6
      },
      projects: [
        {
          id: 4,
          name: "API Development",
          projectId: "PRJ-004",
          status: "Completed",
          progress: 100,
          todayHours: "09:00",
          totalHours: "156:20",
          deadline: "2024-11-15",
          assignedTo: "Rahul Gupta",
          assignedToId: "3",
          assignedToRole: "Backend Developer",
          assignedToImage: "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150&h=150&fit=crop&crop=face&auto=format"
        }
      ]
    },
    {
      id: 4,
      username: "sneha.patel",
      name: "Sneha Patel",
      role: "Project Manager",
      department: "Management",
      email: "sneha@UPFSDA.com",
      phone: "+91 65432 10987",
      profileImage: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&h=150&fit=crop&crop=face&auto=format",
      workMode: "Field",
      isOnline: true,
      attendanceToday: {
        punchIn: "08:45",
        punchOut: null,
        workingHours: "08:20:15",
        status: "Working"
      },
      leaveBalance: {
        remaining: 12,
        used: 8,
        total: 20,
        annual: 8,
        sick: 4,
        personal: 8
      },
      projects: [
        {
          id: 5,
          name: "Team Coordination",
          projectId: "PRJ-005",
          status: "Ongoing",
          progress: 60,
          todayHours: "08:20",
          totalHours: "203:45",
          deadline: "2024-12-31",
          assignedTo: "Sneha Patel",
          assignedToId: "4",
          assignedToRole: "Project Manager",
          assignedToImage: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=150&h=150&fit=crop&crop=face&auto=format"
        }
      ]
    }
  ],

  leaveRequests: [
    {
      id: 1,
      employeeId: 1,
      employeeName: "Lokesh Kumar",
      type: "Annual Leave",
      startDate: "2024-12-20",
      endDate: "2024-12-22",
      days: 3,
      reason: "Family vacation",
      status: "Pending",
      appliedDate: "2024-11-15",
      approver: "Sneha Patel"
    },
    {
      id: 2,
      employeeId: 2,
      employeeName: "Priya Sharma",
      type: "Sick Leave",
      startDate: "2024-11-10",
      endDate: "2024-11-11",
      days: 2,
      reason: "Fever and cold",
      status: "Approved",
      appliedDate: "2024-11-09",
      approver: "Sneha Patel"
    },
    {
      id: 3,
      employeeId: 1,
      employeeName: "Lokesh Kumar",
      type: "Personal Leave",
      startDate: "2024-11-05",
      endDate: "2024-11-05",
      days: 1,
      reason: "Personal work",
      status: "Denied",
      appliedDate: "2024-11-03",
      approver: "Sneha Patel"
    }
  ],

  meetings: [
    {
      id: 1,
      title: "Project Review Meeting",
      date: "2024-11-20",
      time: "10:00 AM",
      attendees: ["Lokesh Kumar", "Priya Sharma", "Sneha Patel"],
      location: "Conference Room A",
      status: "Scheduled"
    },
    {
      id: 2,
      title: "Sprint Planning",
      date: "2024-11-21",
      time: "02:00 PM",
      attendees: ["Priya Sharma", "Rahul Gupta"],
      location: "Virtual",
      status: "Scheduled"
    }
  ],

  holidays: [
    {
      id: 1,
      name: "Christmas Day",
      date: "2024-12-25",
      type: "National Holiday"
    },
    {
      id: 2,
      name: "New Year's Day",
      date: "2025-01-01",
      type: "National Holiday"
    },
    {
      id: 3,
      name: "Independence Day",
      date: "2024-08-15",
      type: "National Holiday"
    }
  ],

  // Admin credentials
  adminCredentials: {
    username: "MasterAdmin",
    password: "pass",
    role: "admin",
    name: "System Administrator"
  },

  // Employee credentials (using first employee as example)
  employeeCredentials: {
    username: "user",
    password: "pass",
    role: "employee",
    employeeData: null // Will be set to mockData.employees[0]
  }
};

// Set employee data reference
mockData.employeeCredentials.employeeData = mockData.employees[0];

// Helper functions for mock API simulation
const mockAPI = {
  // Simulate API delay
  delay: (ms = 500) => new Promise(resolve => setTimeout(resolve, ms)),

  // Get all employees
  getEmployees: async () => {
    await mockAPI.delay();
    return { success: true, data: mockData.employees };
  },

  // Get employee by ID
  getEmployee: async (id) => {
    await mockAPI.delay();
    const employee = mockData.employees.find(emp => emp.id == id);
    return employee ? { success: true, data: employee } : { success: false, error: 'Employee not found' };
  },

  // Get leave requests
  getLeaveRequests: async (employeeId = null) => {
    await mockAPI.delay();
    let requests = mockData.leaveRequests;
    if (employeeId) {
      requests = requests.filter(req => req.employeeId == employeeId);
    }
    return { success: true, data: requests };
  },

  // Get projects
  getProjects: async (employeeId = null) => {
    await mockAPI.delay();
    let projects = [];
    mockData.employees.forEach(emp => {
      if (!employeeId || emp.id == employeeId) {
        projects = projects.concat(emp.projects);
      }
    });
    return { success: true, data: projects };
  },

  // Login simulation
  login: async (username, password) => {
    await mockAPI.delay();
    
    if (username === mockData.adminCredentials.username && password === mockData.adminCredentials.password) {
      return {
        success: true,
        user: mockData.adminCredentials,
        token: 'mock-admin-jwt-token'
      };
    }
    
    if (username === mockData.employeeCredentials.username && password === mockData.employeeCredentials.password) {
      return {
        success: true,
        user: mockData.employeeCredentials,
        token: 'mock-employee-jwt-token'
      };
    }
    
    return { success: false, error: 'Invalid credentials' };
  },

  // Punch in/out simulation
  punchIn: async (employeeId) => {
    await mockAPI.delay();
    const employee = mockData.employees.find(emp => emp.id == employeeId);
    if (employee) {
      const now = new Date();
      employee.attendanceToday.punchIn = now.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit' });
      employee.attendanceToday.status = 'Working';
      return { success: true, data: employee.attendanceToday };
    }
    return { success: false, error: 'Employee not found' };
  },

  punchOut: async (employeeId) => {
    await mockAPI.delay();
    const employee = mockData.employees.find(emp => emp.id == employeeId);
    if (employee) {
      const now = new Date();
      employee.attendanceToday.punchOut = now.toLocaleTimeString('en-US', { hour12: false, hour: '2-digit', minute: '2-digit' });
      employee.attendanceToday.status = 'Completed';
      return { success: true, data: employee.attendanceToday };
    }
    return { success: false, error: 'Employee not found' };
  }
};

// Make mockData and mockAPI available globally
window.mockData = mockData;
window.mockAPI = mockAPI;