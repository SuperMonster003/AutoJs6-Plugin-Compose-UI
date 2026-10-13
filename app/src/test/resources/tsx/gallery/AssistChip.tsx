"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.AssistChip label='Set a reminder' leadingIcon={<compose.Icon name='Filled.DateRange' />} onClick={function () { toast('Assist'); }} />
        </compose.Column>
    );
});
