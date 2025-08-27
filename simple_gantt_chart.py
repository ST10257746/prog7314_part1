import plotly.figure_factory as ff
import plotly.graph_objects as go
import pandas as pd

def create_gantt_chart():
    # Define the project data
    tasks = [
        # Phase 1: Foundation & Setup (Weeks 1-2)
        dict(Task="1.1 Project Environment Setup", Start='2024-01-01', Finish='2024-01-02', Resource='Phase 1'),
        dict(Task="1.2 Database Design & API Planning", Start='2024-01-03', Finish='2024-01-05', Resource='Phase 1'),
        dict(Task="2.1 Database Implementation", Start='2024-01-08', Finish='2024-01-10', Resource='Phase 1'),
        dict(Task="2.2 API Development", Start='2024-01-11', Finish='2024-01-12', Resource='Phase 1'),
        
        # Phase 2: Authentication & User Management (Weeks 3-4)
        dict(Task="3.1 User Registration & Login", Start='2024-01-15', Finish='2024-01-17', Resource='Phase 2'),
        dict(Task="3.2 Single Sign-On (SSO) Integration", Start='2024-01-18', Finish='2024-01-19', Resource='Phase 2'),
        dict(Task="4.1 Biometric Authentication", Start='2024-01-22', Finish='2024-01-24', Resource='Phase 2'),
        dict(Task="4.2 Settings Management", Start='2024-01-25', Finish='2024-01-26', Resource='Phase 2'),
        
        # Phase 3: Core Features Development (Weeks 5-8)
        dict(Task="5.1 Home Fragment Enhancement", Start='2024-01-29', Finish='2024-01-31', Resource='Phase 3'),
        dict(Task="5.2 Progress Tracking", Start='2024-02-01', Finish='2024-02-02', Resource='Phase 3'),
        dict(Task="6.1 Workout Planning", Start='2024-02-05', Finish='2024-02-07', Resource='Phase 3'),
        dict(Task="6.2 Session Tracking", Start='2024-02-08', Finish='2024-02-09', Resource='Phase 3'),
        dict(Task="7.1 Nutrition Tracking", Start='2024-02-12', Finish='2024-02-14', Resource='Phase 3'),
        dict(Task="7.2 Nutrition Analysis", Start='2024-02-15', Finish='2024-02-16', Resource='Phase 3'),
        dict(Task="8.1 Offline Mode Implementation", Start='2024-02-19', Finish='2024-02-21', Resource='Phase 3'),
        dict(Task="8.2 Sync Mechanism", Start='2024-02-22', Finish='2024-02-23', Resource='Phase 3'),
        
        # Phase 4: Advanced Features (Weeks 9-10)
        dict(Task="9.1 Push Notification System", Start='2024-02-26', Finish='2024-02-28', Resource='Phase 4'),
        dict(Task="9.2 Real-time Updates", Start='2024-02-29', Finish='2024-03-01', Resource='Phase 4'),
        dict(Task="10.1 Localization Setup", Start='2024-03-04', Finish='2024-03-06', Resource='Phase 4'),
        dict(Task="10.2 UI Adaptation", Start='2024-03-07', Finish='2024-03-08', Resource='Phase 4'),
        
        # Phase 5: Testing & Quality Assurance (Weeks 11-12)
        dict(Task="11.1 Unit Testing", Start='2024-03-11', Finish='2024-03-13', Resource='Phase 5'),
        dict(Task="11.2 Integration Testing", Start='2024-03-14', Finish='2024-03-15', Resource='Phase 5'),
        dict(Task="12.1 User Acceptance Testing", Start='2024-03-18', Finish='2024-03-20', Resource='Phase 5'),
        dict(Task="12.2 Deployment Preparation", Start='2024-03-21', Finish='2024-03-22', Resource='Phase 5'),
    ]
    
    # Create DataFrame
    df = pd.DataFrame(tasks)
    
    # Define colors for each phase
    colors = {
        'Phase 1': '#1f77b4',  # Blue
        'Phase 2': '#ff7f0e',  # Orange
        'Phase 3': '#2ca02c',  # Green
        'Phase 4': '#d62728',  # Red
        'Phase 5': '#9467bd'   # Purple
    }
    
    # Create the Gantt chart
    fig = ff.create_gantt(df, 
                         colors=colors,
                         index_col='Resource',
                         title='Android Fitness App - Project Timeline',
                         show_colorbar=True,
                         group_tasks=True,
                         showgrid_x=True,
                         showgrid_y=True)
    
    # Update layout for better appearance
    fig.update_layout(
        title={
            'text': 'Android Fitness App - Project Timeline & Gantt Chart',
            'y':0.95,
            'x':0.5,
            'xanchor': 'center',
            'yanchor': 'top',
            'font': {'size': 24, 'color': '#2c3e50'}
        },
        font=dict(size=12, color='#2c3e50'),
        plot_bgcolor='white',
        paper_bgcolor='white',
        height=800,
        width=1200,
        xaxis=dict(
            title='Timeline',
            titlefont=dict(size=16, color='#2c3e50'),
            tickfont=dict(size=12, color='#2c3e50'),
            gridcolor='#ecf0f1',
            showgrid=True
        ),
        yaxis=dict(
            title='Tasks',
            titlefont=dict(size=16, color='#2c3e50'),
            tickfont=dict(size=12, color='#2c3e50'),
            gridcolor='#ecf0f1',
            showgrid=True
        )
    )
    
    return fig

def create_phase_summary():
    """Create a summary chart showing phase durations"""
    phases = ['Phase 1', 'Phase 2', 'Phase 3', 'Phase 4', 'Phase 5']
    durations = [2, 2, 4, 2, 2]  # weeks
    colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd']
    
    fig = go.Figure(data=[
        go.Bar(
            x=phases,
            y=durations,
            marker_color=colors,
            text=[f'{d} weeks' for d in durations],
            textposition='auto',
        )
    ])
    
    fig.update_layout(
        title='Project Phase Duration Summary',
        xaxis_title='Project Phases',
        yaxis_title='Duration (Weeks)',
        height=400,
        width=800,
        plot_bgcolor='white',
        paper_bgcolor='white',
        font=dict(size=14, color='#2c3e50')
    )
    
    return fig

def main():
    print("Generating Android Fitness App Project Gantt Chart...")
    
    # Create the main Gantt chart
    gantt_fig = create_gantt_chart()
    
    # Create phase summary
    summary_fig = create_phase_summary()
    
    # Save the charts as HTML files for easy viewing and screenshot
    gantt_fig.write_html("android_fitness_gantt_chart.html")
    summary_fig.write_html("phase_summary_chart.html")
    
    print("Charts generated successfully!")
    print("Files created:")
    print("- android_fitness_gantt_chart.html (Main Gantt Chart)")
    print("- phase_summary_chart.html (Phase Duration Summary)")
    print("\nOpen these HTML files in your browser to view and screenshot the charts.")
    print("The main Gantt chart is in 'android_fitness_gantt_chart.html'")
    
    # Also save as static images
    gantt_fig.write_image("android_fitness_gantt_chart.png", width=1200, height=800)
    summary_fig.write_image("phase_summary_chart.png", width=800, height=400)
    
    print("PNG images also saved for direct screenshot use.")

if __name__ == "__main__":
    main()
