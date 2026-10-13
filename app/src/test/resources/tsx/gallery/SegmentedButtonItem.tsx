"ui";

let bold = compose.state(true);
let italic = compose.state(false);

compose.mount(function () {
    return (
        <compose.Column padding={16} spacing={12}>
            <compose.SegmentedButton key='filters' mode='multi'>
                <compose.SegmentedButtonItem key='bold' label='Bold' selected={bold.value} onSelectedChange={function (value) { bold.value = value; }} />
                <compose.SegmentedButtonItem key='italic' label='Italic' selected={italic.value} onSelectedChange={function (value) { italic.value = value; }} />
            </compose.SegmentedButton>
        </compose.Column>
    );
});
