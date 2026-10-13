"ui";

let choice = compose.state('day');

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.SegmentedButton key='choice' mode='single'>
                <compose.SegmentedButtonItem key='day' label='Day' selected={choice.value === 'day'} onSelectedChange={function () { choice.value = 'day'; }} />
                <compose.SegmentedButtonItem key='week' label='Week' selected={choice.value === 'week'} onSelectedChange={function () { choice.value = 'week'; }} />
                <compose.SegmentedButtonItem key='month' label='Month' selected={choice.value === 'month'} onSelectedChange={function () { choice.value = 'month'; }} />
            </compose.SegmentedButton>
        </compose.Column>
    );
});
