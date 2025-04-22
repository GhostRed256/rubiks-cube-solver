import cv2
import numpy as np
import mediapipe as mp
from kociemba import solve
import pygame
import time

class RubiksCubeSolver:
    def __init__(self):
        self.mp_hands = mp.solutions.hands
        self.hands = self.mp_hands.Hands(
            static_image_mode=False,
            max_num_hands=1,
            min_detection_confidence=0.7,
            min_tracking_confidence=0.5
        )
        self.mp_draw = mp.solutions.drawing_utils
        
        # Initialize webcam
        self.cap = cv2.VideoCapture(0)
        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)
        
        # Colors for cube faces
        self.colors = {
            'white': (255, 255, 255),
            'yellow': (0, 255, 255),
            'red': (0, 0, 255),
            'orange': (0, 165, 255),
            'blue': (255, 0, 0),
            'green': (0, 255, 0)
        }
        
        # Cube state representation
        self.cube_state = {
            'U': [],  # Up face
            'D': [],  # Down face
            'L': [],  # Left face
            'R': [],  # Right face
            'F': [],  # Front face
            'B': []   # Back face
        }
        
        # Initialize pygame for displaying solution
        pygame.init()
        self.screen = pygame.display.set_mode((800, 600))
        pygame.display.set_caption("Rubik's Cube Solver")
        
    def detect_cube_colors(self, frame):
        # Convert frame to HSV for better color detection
        hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
        
        # Define color ranges
        color_ranges = {
            'white': ([0, 0, 200], [180, 30, 255]),
            'yellow': ([20, 100, 100], [30, 255, 255]),
            'red': ([0, 100, 100], [10, 255, 255]),
            'orange': ([10, 100, 100], [20, 255, 255]),
            'blue': ([100, 100, 100], [130, 255, 255]),
            'green': ([40, 100, 100], [80, 255, 255])
        }
        
        # Create a grid for cube face detection
        height, width = frame.shape[:2]
        grid_size = 3
        cell_height = height // grid_size
        cell_width = width // grid_size
        
        detected_colors = []
        
        for i in range(grid_size):
            for j in range(grid_size):
                cell = hsv[i*cell_height:(i+1)*cell_height, j*cell_width:(j+1)*cell_width]
                dominant_color = self.get_dominant_color(cell, color_ranges)
                detected_colors.append(dominant_color)
                
        return detected_colors
    
    def get_dominant_color(self, cell, color_ranges):
        max_pixels = 0
        dominant_color = None
        
        for color, (lower, upper) in color_ranges.items():
            mask = cv2.inRange(cell, np.array(lower), np.array(upper))
            pixel_count = np.sum(mask > 0)
            
            if pixel_count > max_pixels:
                max_pixels = pixel_count
                dominant_color = color
                
        return dominant_color
    
    def display_solution(self, solution):
        self.screen.fill((0, 0, 0))
        font = pygame.font.Font(None, 36)
        
        # Split solution into moves
        moves = solution.split()
        
        # Display moves
        for i, move in enumerate(moves):
            text = font.render(move, True, (255, 255, 255))
            self.screen.blit(text, (50, 50 + i*40))
            
        pygame.display.flip()
    
    def run(self):
        while True:
            ret, frame = self.cap.read()
            if not ret:
                break
                
            # Flip the frame horizontally for a later selfie-view display
            frame = cv2.flip(frame, 1)
            
            # Convert the BGR image to RGB
            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            
            # Process the frame and detect hands
            results = self.hands.process(rgb_frame)
            
            # Draw hand landmarks
            if results.multi_hand_landmarks:
                for hand_landmarks in results.multi_hand_landmarks:
                    self.mp_draw.draw_landmarks(
                        frame, hand_landmarks, self.mp_hands.HAND_CONNECTIONS)
            
            # Detect cube colors
            detected_colors = self.detect_cube_colors(frame)
            
            # Draw grid on frame
            height, width = frame.shape[:2]
            grid_size = 3
            cell_height = height // grid_size
            cell_width = width // grid_size
            
            for i in range(grid_size):
                for j in range(grid_size):
                    cv2.rectangle(frame, 
                                (j*cell_width, i*cell_height),
                                ((j+1)*cell_width, (i+1)*cell_height),
                                (255, 255, 255), 1)
            
            # Display the frame
            cv2.imshow('Rubik\'s Cube Solver', frame)
            
            # Handle keyboard input
            key = cv2.waitKey(1) & 0xFF
            if key == ord('q'):
                break
            elif key == ord('s'):
                # Capture current face
                print("Captured face colors:", detected_colors)
            
        self.cap.release()
        cv2.destroyAllWindows()
        pygame.quit()

if __name__ == "__main__":
    solver = RubiksCubeSolver()
    solver.run() 