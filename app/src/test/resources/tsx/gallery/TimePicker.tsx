"ui";

let hour = compose.state(9);
let minute = compose.state(30);

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={8}>
            <compose.Text key='value' text={hour.value + ':' + (minute.value < 10 ? '0' : '') + minute.value} />
            <compose.TimePicker key='time' hour={hour.value} minute={minute.value} is24Hour={true} onTimeChange={function (h, m) { hour.value = h; minute.value = m; }} />
        </compose.Column>
    );
});
