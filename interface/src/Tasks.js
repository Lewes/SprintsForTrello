import React, {useEffect, useState} from 'react';

function Tasks(props) {
    const [tasks, setTasks] = useState([]);

    useEffect(() => {
        if(props.sprint == null ||
            props.sprint.id == null) {
            return;
        }

        fetch("http://localhost:8080/sprints/" + props.sprint.id + "/tasks", {method: 'GET'})
            .then(res => {
                if(!res.ok) {
                    throw new Error("No current sprint")
                }

                return res.json();
            })
            .catch(error => {
                return null;
            }).then(res => {
                setTasks(res);
        });
    }, [props.sprint]);

    if(props.sprint == null) {
        return null;
    }

    if(tasks == null ||
        tasks.length == 0) {
        return (
            <p>There are no tasks in the sprint.</p>
        );
    }

    return (
        <table className="Tasks">
            <tr className="Header">
                <th>Task</th>
                <th>Points</th>
                <th>Status</th>
            </tr>
            {tasks.map(task => {
                return (
                    <tr>
                        <td>{task.name}</td>
                        <td>{task.points}</td>
                        <td>{task.status}</td>
                    </tr>
                );
            })}
        </table>
    );
}

export default Tasks;