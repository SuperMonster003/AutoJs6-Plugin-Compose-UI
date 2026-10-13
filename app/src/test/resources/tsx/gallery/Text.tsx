"ui";

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={8}>
            <compose.Text key='headline' text='Headline' style='headlineSmall' />
            <compose.Text key='body' text='Body text with a custom color' color='#1565C0' />
            <compose.Text key='clipped' text='A very long single line that is clipped with an ellipsis at the end' maxLines={1} overflow='ellipsis' />
        </compose.Column>
    );
});
