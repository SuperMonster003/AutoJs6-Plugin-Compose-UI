"ui";

let date = compose.state(null);

compose.mount(function () {
    return (
        <compose.Column padding={16}>
            <compose.DatePicker key='date' selectedDateMillis={date.value} displayMode='picker' onDateChange={function (millis) { date.value = millis; }} />
        </compose.Column>
    );
});
